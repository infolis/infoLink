package io.github.infolis.commandLine;

import io.github.infolis.algorithm.Algorithm;
import io.github.infolis.algorithm.Indexer;
import io.github.infolis.algorithm.SearchTermPosition;
import io.github.infolis.algorithm.TextExtractorAlgorithm;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.FileResolverFactory;
import io.github.infolis.model.BootstrapStrategy;
import io.github.infolis.model.Execution;
import io.github.infolis.model.MetaDataExtractingStrategy;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.util.SerializationUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
public class CommandLineExecuter {
	
	private static final Logger log = LoggerFactory.getLogger(CommandLineExecuter.class);

    static protected DataStoreClient dataStoreClient;
    static protected FileResolver fileResolver;

    public static void parseJson(Path jsonPath, Path outputDir) throws FileNotFoundException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, IOException {
        JsonReader reader = Json.createReader(Files.newBufferedReader(jsonPath, Charset.forName("UTF-8")));
        JsonObject o = reader.readObject();

        String wrongParameter = checkJsonFile(o);
        if(!wrongParameter.isEmpty()) {
            System.out.println(String.format("ERROR: %s is no parameter name", wrongParameter));
        }

        Execution e = new Execution();
        //TODO: any better solution? e.g. to indicate the strategy as method parameter?
        dataStoreClient = DataStoreClientFactory.create(DataStoreStrategy.TEMPORARY);
        fileResolver = FileResolverFactory.create(DataStoreStrategy.TEMPORARY);

        //iterate through the entries in the JSON file
        for (Entry<String, JsonValue> values : o.entrySet()) {
            switch (values.getValue().getValueType()) {
                case STRING:
                case NUMBER:
                case TRUE:
                case FALSE:
                    //algorithm has to be handled as a special case since we need to find the class
                    if (values.getKey().equals("algorithm")) {
                        String algorithmName = values.getValue().toString();
                        algorithmName = algorithmName.replace("\"", "");
                        if (!algorithmName.startsWith("io.github.infolis.algorithm")) {
                            algorithmName += "io.github.infolis.algorithm." + algorithmName;
                        }
                        Class<? extends Algorithm> algoClass = (Class<? extends Algorithm>) Class.forName(algorithmName);
                        e.setAlgorithm(algoClass);
                        break;
                    }
                    if (values.getKey().equals("bootstrapStrategy")) {
                        BootstrapStrategy b = BootstrapStrategy.valueOf(values.getValue().toString().replace("\"", ""));
                        e.setBootstrapStrategy(b);
                        break;
                    }
                    if (values.getKey().equals("metaDataExtractingStrategy")) {
                        MetaDataExtractingStrategy mde = MetaDataExtractingStrategy.valueOf(values.getValue().toString().replace("\"", ""));
                        e.setMetaDataExtractingStrategy(mde);
                        break;
                    }
                    //inputFiles need to be handled as a special case since we need to create the 
                    //files first and post them and convert them if necessary
                    if (values.getKey().equals("inputFiles")) {
                        String dir = values.getValue().toString().replace("\"", "");
                        List<String> fileUris = postFiles(dir, dataStoreClient, fileResolver);
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
                        JsonString stringEntry = array.getJsonString(i);
                        listEntries.add(stringEntry.getString());
                    }
                    e.setProperty(values.getKey(), listEntries);
                    break;
                default:
                    System.err.println("WARNING: Unhandled value type " + values.getValue().getValueType());
                    break;
            }
        }
        if (e.getAlgorithm().equals(SearchTermPosition.class)) {
        	Execution indexerExecution = new Execution();
        	indexerExecution.setAlgorithm(Indexer.class);
        	indexerExecution.setInputFiles(e.getInputFiles());
        	indexerExecution.setPhraseSlop(0);
        	dataStoreClient.post(Execution.class, indexerExecution);
        	indexerExecution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        	e.setInputDirectory(indexerExecution.getOutputDirectory());
        }
        dataStoreClient.post(Execution.class, e);
        e.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        dataStoreClient.dump(outputDir);
    }

    /**
     * checks whether all the variables in the Json file
     * are indeed variables which can be set in the execution.
     * 
     * @param o
     * @return 
     */
    private static String checkJsonFile(JsonObject o) {
        for (Entry<String, JsonValue> values : o.entrySet()) {
            try {
                Execution e = new Execution();
                e.getClass().getDeclaredField(values.getKey());
            }catch(NoSuchFieldException ex) {
                return values.getKey();
            }
        }
        return "";
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
                    Algorithm algo = convertExec.instantiateAlgorithm(dataStoreClient, fileResolver);
                    algo.run();
                    // Set the inputFile to the file we just created
                    InfolisFile convertedInputFile = dataStoreClient.get(InfolisFile.class, convertExec.getOutputFiles().get(0));
                    txtUris.add(convertedInputFile.getUri());
                }
            } else {
                txtUris.add(inputFileURI);
            }
        }
        return txtUris;
    }

    public static List<String> postFiles(String dirStr, DataStoreClient dsc, FileResolver rs) throws IOException {
        List<String> uris = new ArrayList<>();
        Path dir = Paths.get(dirStr);
        Iterator<Path> dirIter = Files.newDirectoryStream(dir).iterator();
		while (dirIter.hasNext()) {
			Path f = dirIter.next();
			Path tempFile = Files.createTempFile("infolis-", ".pdf");
            InfolisFile inFile = new InfolisFile();

            InputStream inputStream = Files.newInputStream(f);

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
            inputStream.close();
        }
        return uris;
    }

    public static void usage(String problem) {
        System.out.println(String.format("%s <json-path> <output-dir>", CommandLineExecuter.class.getSimpleName()));
        if (null != problem) {
            System.out.println(String.format("ERROR: %s", problem));
        }
        System.exit(1);
    }

    public static void main(String args[]) throws FileNotFoundException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, IOException {
        if (args.length < 2) {
            usage("Not enough arguments");
        }

        Path jsonPath = Paths.get(args[0]);
        if (!Files.exists(jsonPath)) {
            usage("JSON doesn't exist");
        }

        Path outputDir = Paths.get(args[1]);
        if (Files.exists(outputDir)) {
            System.err.println("WARNING: Output directory already exists, make sure it is empty.\nPress enter to continue or CTRL-C to exit");
            System.in.read();
        } else {
            Files.createDirectories(outputDir);
        }

        parseJson(jsonPath, outputDir);
    }
}
