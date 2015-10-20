package io.github.infolis.commandLine;

import io.github.infolis.algorithm.Algorithm;
import io.github.infolis.algorithm.BaseAlgorithm;
import io.github.infolis.algorithm.IllegalAlgorithmArgumentException;
import io.github.infolis.algorithm.TextExtractorAlgorithm;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.FileResolverFactory;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.util.SerializationUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

/**
 *
 * @author domi
 */
public class CommandLineExecuter {
    
    static protected DataStoreClient dataStoreClient;
    static protected FileResolver fileResolver;

    public static void parseJson(String fileName) throws FileNotFoundException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, IOException {
        File jsonFile = new File(fileName);
        JsonReader reader = Json.createReader(new BufferedReader(new FileReader(jsonFile)));
        JsonObject o = reader.readObject();

        Execution e = new Execution();
        dataStoreClient = DataStoreClientFactory.create(DataStoreStrategy.TEMPORARY);
        fileResolver = FileResolverFactory.create(DataStoreStrategy.TEMPORARY);
        
        for (Entry<String, JsonValue> values : o.entrySet()) {
            switch (values.getValue().getValueType()) {
                case STRING:
                case NUMBER:
                case TRUE:
                case FALSE:    
                    //algorithm has to be handled as a special case since we need to find the class
                    if (values.getKey().equals("algorithm")) {
                        String algorithmName = values.getValue().toString();
                        algorithmName = algorithmName.replace(fileName, fileName).replace("\"", "");
                        if (!algorithmName.startsWith("io.github.infolis.algorithm")) {
                            algorithmName += "io.github.infolis.algorithm." + algorithmName;
                        }
                        Class<? extends Algorithm> algoClass = determineClass(algorithmName);
                        e.setAlgorithm(algoClass);
                        break;
                    }
                    //inputFiles need to be handled as a special case since we need to create the 
                    //files first and post them and convert them if necessary
                    if(values.getKey().equals("inputFiles")) {
                        String dir = values.getValue().toString().replace("\"", "");
                        List<String> fileUris = postFiles(dir,dataStoreClient,fileResolver);                        
                        e.setInputFiles(convertPDF(fileUris));
                        break;
                    }                    
                    //all other fields are just set
                    e.setProperty(values.getKey(), values.getValue().toString().replace("\"", ""));
                    break;
                //for arrays we first have to create a list    
                case ARRAY:
                    JsonArray array = (JsonArray) values.getValue();
                    List<String> listEntries = new ArrayList<>();
                    for (int i = 0; i < array.size(); i++) {
                        JsonString stringSeed = array.getJsonString(i);                        
                        listEntries.add(stringSeed.getString());
                    }
                    e.setProperty(values.getKey(), listEntries);
                    break;
            }
        }       
        dataStoreClient.post(Execution.class, e);        
        e.instantiateAlgorithm(dataStoreClient, fileResolver).run();

    }
    
    private static List<String> convertPDF(List<String> uris) {
        List<String> txtUris = new ArrayList<>();
        for (String inputFileURI : uris) {
            InfolisFile inputFile = dataStoreClient.get(InfolisFile.class, inputFileURI);
            if (null == inputFile) {
                throw new RuntimeException("File was not registered with the data store: " + inputFileURI);
            }
            if (null == inputFile.getMediaType()) {
                throw new RuntimeException("File has no mediaType: " + inputFileURI);
            }
            // if the input file is not a text file
            if (!inputFile.getMediaType().startsWith("text/plain")) {
                // if the input file is a PDF file, convert it
                if (inputFile.getMediaType().startsWith("application/pdf")) {
                    Execution convertExec = new Execution();
                    convertExec.setAlgorithm(TextExtractorAlgorithm.class);
                    convertExec.setInputFiles(Arrays.asList(inputFile.getUri()));
                    // TODO wire this more efficiently so files are stored temporarily
                    Algorithm algo = convertExec.instantiateAlgorithm(dataStoreClient,fileResolver);
                    algo.run();
                    // Set the inputFile to the file we just created
                    InfolisFile convertedInputFile = dataStoreClient.get(InfolisFile.class, convertExec.getOutputFiles().get(0));
                    txtUris.add(convertedInputFile.getUri());
                } 
            }
            else {
                txtUris.add(inputFileURI);
            }
        }
        return txtUris;
    }

    private static Class<? extends Algorithm> determineClass(String algoName) {
        Set<Class<? extends Object>> clazzSet = getClassSet("io.github.infolis.algorithm");
        for (Class<? extends Object> clazz : clazzSet) {
            if (algoName.equals(clazz.getName())) {
                return (Class<? extends Algorithm>) clazz;
            }
        }
        return null;
    }

    public static Set<Class<? extends Object>> getClassSet(String pkg) {
        // prepare reflection, include direct subclass of Object.class
        Reflections reflections = new Reflections(new ConfigurationBuilder().setScanners(new SubTypesScanner(false), new ResourcesScanner())
                .setUrls(ClasspathHelper.forClassLoader(ClasspathHelper.classLoaders(new ClassLoader[0])))
                .filterInputsBy(new FilterBuilder().includePackage(pkg)));

        return reflections.getSubTypesOf(Object.class);
    }

    public static List<String> postFiles(String dir, DataStoreClient dsc, FileResolver rs) throws IOException {
        List<String> uris = new ArrayList<>();
        File dirFile = new File(dir);
        for (File f : dirFile.listFiles()) {
            
            Path tempFile = Files.createTempFile("infolis-", ".pdf");
            InfolisFile inFile = new InfolisFile();

            FileInputStream inputStream = new FileInputStream(f.getAbsolutePath());

            int numberBytes = inputStream.available();
            byte pdfBytes[] = new byte[numberBytes];
            inputStream.read(pdfBytes);

            IOUtils.write(pdfBytes, Files.newOutputStream(tempFile));

            inFile.setFileName(tempFile.toString());
            inFile.setMd5(SerializationUtils.getHexMd5(pdfBytes));
            inFile.setMediaType("application/pdf");
            inFile.setFileStatus("AVAILABLE");

            try {
                OutputStream os = rs.openOutputStream(inFile);
                IOUtils.write(pdfBytes, os);
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            dsc.post(InfolisFile.class, inFile);       
            uris.add(inFile.getUri());
        }
        return uris;
    }
    
    public static void main(String args[]) throws FileNotFoundException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, IOException {
        parseJson(args[0]);
    }
}
