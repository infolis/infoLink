package io.github.infolis.commandLine;

import io.github.infolis.algorithm.Algorithm;
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
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.commons.io.IOUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
public class CommandLineExecuter {
	
	private static final Logger log = LoggerFactory.getLogger(CommandLineExecuter.class);
	//TODO: any better solution? e.g. to indicate the strategy as method parameter?
    private static DataStoreClient dataStoreClient = DataStoreClientFactory.create(DataStoreStrategy.TEMPORARY);
    private static FileResolver fileResolver = FileResolverFactory.create(DataStoreStrategy.LOCAL);
    private static final Path PWD = Paths.get(".").toAbsolutePath().normalize();

    @Option(name="--text-dir",usage="text-directory",metaVar="TEXTDIR")
    private Path textDir = PWD.resolve("text");

    @Option(name="--input-dir",usage="output-directory",metaVar="INPUTDIR",required=true)
    private Path inputDir;
    
    @Option(name="--output-dir",usage="output-directory",metaVar="OUTPUTDIR")
    private Path outputDir = PWD.resolve("output");

    @Option(name="--json",usage="json",metaVar="JSON",required=true)
    private Path json;

    public void parseJson() throws FileNotFoundException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, IOException {
        JsonReader reader = Json.createReader(Files.newBufferedReader(json, Charset.forName("UTF-8")));
        JsonObject o = reader.readObject();

        String wrongParameter = checkJsonFile(o);
        if(!wrongParameter.isEmpty()) {
            System.out.println(String.format("ERROR: %s is no parameter name", wrongParameter));
            System.exit(1);
        }

        Execution e = new Execution();

        //inputFiles need to be handled as a special case since we need to create the 
        e.setInputFiles(convertPDF(postFiles()));

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
                        @SuppressWarnings("unchecked")
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

    private List<String> convertPDF(List<String> uris) {
        List<String> txtUris = new ArrayList<>();
        for (String inputFileURI : uris) {
            InfolisFile inputFile = dataStoreClient.get(InfolisFile.class, inputFileURI);
            if (null == inputFile) {
                throwCLI("File was not registered with the data store: " + inputFileURI, null);
            }
            if (null == inputFile.getMediaType()) {
                throwCLI("File has no mediaType: " + inputFileURI, null);
            }
            if (inputFile.getMediaType().startsWith("text/plain")) {
                txtUris.add(inputFileURI);
                continue;
            }
            // if the input file is a PDF file, convert it
            if (inputFile.getMediaType().startsWith("application/pdf")) {
            	Execution convertExec = new Execution();
            	convertExec.setAlgorithm(TextExtractorAlgorithm.class);
            	convertExec.setOutputDirectory(outputDir.toString());
            	convertExec.setInputFiles(Arrays.asList(inputFile.getUri()));
            	Algorithm algo = convertExec.instantiateAlgorithm(dataStoreClient, fileResolver);
            	algo.run();
            	txtUris.add(convertExec.getOutputFiles().get(0));
            }
        }
        return txtUris;
    }

    public List<String> postFiles() {
    	List<InfolisFile> infolisFiles = new ArrayList<>();
		DirectoryStream<Path> dirStream = null;
		try {
			dirStream = Files.newDirectoryStream(inputDir);
		} catch (IOException e) {
			throwCLI("Couldn't list directory contents of " + inputDir, e);
		}
		for (Path file : dirStream) {
            InfolisFile infolisFile = new InfolisFile();

            try (InputStream inputStream = Files.newInputStream(file)) {
            	byte[] pdfBytes = IOUtils.toByteArray(inputStream);
            	infolisFile.setMd5(SerializationUtils.getHexMd5(pdfBytes));
            } catch (IOException e) {
            	throwCLI("Could not read file " + file, e);
            };

            infolisFile.setFileName(file.toString());
            infolisFile.setMediaType("application/pdf");
            infolisFile.setFileStatus("AVAILABLE");
            infolisFiles.add(infolisFile);
        }
		return dataStoreClient.post(InfolisFile.class, infolisFiles);
    }
    
    private static void throwCLI(String msg, Exception e)
    {
    	System.err.println("**ERROR** " + msg);
    	if (null != e)
    	{
    		e.printStackTrace();
    	}
    	System.exit(1);
    }

    public void doMain(String args[]) throws FileNotFoundException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, IOException {
    	 CmdLineParser parser = new CmdLineParser(this, ParserProperties.defaults().withUsageWidth(120));
    	 try {
			parser.parseArgument(args);
            Files.createDirectories(outputDir);
            Files.createDirectories(textDir);
            parseJson();
		} catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("java " + getClass().getSimpleName() +" [options...]");
            parser.printUsage(System.err);
		}
    }
    
    public static void main(String args[]) {
    	try {
			new CommandLineExecuter().doMain(args);
		} catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | IOException e) {
			throwCLI("doMain", e);
		}
    }
}
