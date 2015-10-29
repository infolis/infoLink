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
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
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
 * CLI to Infolis to make it easy to run an execution and store its results in a JSON file.
 * 
 */
public class CommandLineExecuter {

    private static final Logger log = LoggerFactory.getLogger(CommandLineExecuter.class);

    private DataStoreClient dataStoreClient = DataStoreClientFactory.create(DataStoreStrategy.TEMPORARY);
    private FileResolver fileResolver = FileResolverFactory.create(DataStoreStrategy.LOCAL);

    @Option(name = "--text-dir", usage = "Directory containing *.txt", metaVar = "TEXTDIR", required = true)
    private Path textDir;

    @Option(name = "--pdf-dir", usage = "Directory containing *.pdf", metaVar = "PDFDIR")
    private Path pdfDir;

    @Option(name = "--db-dir", usage = "Directory to hold JSON database dump", metaVar = "OUTPUTDIR", required = true)
    private Path dbDir;

    @Option(name = "--json", usage = "json", metaVar = "Execution as JSON", required = true)
    private Path json;

    @SuppressWarnings("unchecked")
    private void setExecutionFromJSON(JsonObject jsonObject, Execution exec) {
        try {
            // iterate through the entries in the JSON file
            for (Entry<String, JsonValue> values : jsonObject.entrySet()) {
                switch (values.getValue().getValueType()) {
                case STRING:
                case NUMBER:
                case TRUE:
                case FALSE:
                    // algorithm has to be handled as a special case since we need
                    // to find the class
                    if (values.getKey().equals("algorithm")) {
                        String algorithmName = values.getValue().toString();
                        algorithmName = algorithmName.replace("\"", "");
                        if (!algorithmName.startsWith("io.github.infolis.algorithm")) {
                            algorithmName += "io.github.infolis.algorithm." + algorithmName;
                        }
                        try {
                            Class<? extends Algorithm> algoClass;
                            algoClass = (Class<? extends Algorithm>) Class.forName(algorithmName);
                            exec.setAlgorithm(algoClass);
                        } catch (ClassNotFoundException |ClassCastException e1) {
                            throwCLI("No such algorithm: " + algorithmName);
                        }
                        break;
                    }
                    // TODO generic solution for enums?
                    if (values.getKey().equals("bootstrapStrategy")) {
                        BootstrapStrategy b = BootstrapStrategy.valueOf(values.getValue().toString().replace("\"", ""));
                        exec.setBootstrapStrategy(b);
                        break;
                    }
                    if (values.getKey().equals("metaDataExtractingStrategy")) {
                        MetaDataExtractingStrategy mde = MetaDataExtractingStrategy.valueOf(values.getValue().toString().replace("\"", ""));
                        exec.setMetaDataExtractingStrategy(mde);
                        break;
                    }
                    // all other fields are just set
                    exec.setProperty(values.getKey(), values.getValue().toString().replace("\"", ""));
                    break;
                    // for arrays we first have to create a list
                case ARRAY:
                    JsonArray array = (JsonArray) values.getValue();
                    List<String> listEntries = new ArrayList<>();
                    for (int i = 0; i < array.size(); i++) {
                        JsonString stringEntry = array.getJsonString(i);
                        listEntries.add(stringEntry.getString());
                    }
                    exec.setProperty(values.getKey(), listEntries);
                    break;
                default:
                    throwCLI("Unhandled value type " + values.getValue().getValueType() + " for JSON key " + values.getKey());
                    break;
                }
            }
        } catch (NoSuchFieldException|IllegalAccessException e) {
            throwCLI("No such field", e);
        }
    }

    private void doExecute(Execution exec) {
        dataStoreClient.post(Execution.class, exec);
        exec.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        dataStoreClient.dump(dbDir);
    }

    private void setExecutionInputFiles(Execution exec) throws IOException {
        if (null == pdfDir || ! Files.exists(pdfDir)) {
            if (null == textDir || ! Files.exists(textDir)) {
                log.debug("Case 4");
                throwCLI("Neither PDFDIR nor TEXTDIR exist");
            } else {
                log.debug("Case 3");
                if (! Files.newDirectoryStream(textDir).iterator().hasNext()) {
                    throwCLI("No PDFDIR specified, TEXTDIR specified, but empty.");
                }
                exec.setInputFiles(postFiles(textDir, "text/plain"));
            }
        } else {
            if (null == textDir || ! Files.exists(textDir)) {
                log.debug("Case 2");
                Files.createDirectories(textDir);
                exec.setInputFiles(convertPDF(postFiles(pdfDir, "application/pdf")));
            } else {
                System.err.println("WARNING: Both --text-dir '" + textDir + "' and --pdf-dir '" + pdfDir + "' exist. Convert from PDF anyway?\n\t<Ctrl-C> to quit, <Enter> to continue");
                try {
                    System.in.read();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log.debug("Case 1");
                exec.setInputFiles(convertPDF(postFiles(pdfDir, "application/pdf")));
            }
        }
        log.debug("Here i be");
    }

    /**
     * checks whether all the variables in the Json file are indeed variables
     * which can be set in the execution.
     * 
     * @param o
     * @return
     */
    private void checkJsonFile(JsonObject o) {
        List<String> badFields = new ArrayList<>();
        Execution testExecution = new Execution();
        for (Entry<String, JsonValue> values : o.entrySet()) {
            if (values.getKey().equals("inputFiles")) {
                throwCLI("Do not specify inputFiles in JSON, it will be overridden [in " + json + "]");
            }
            try {
                testExecution.getClass().getDeclaredField(values.getKey());
            } catch (NoSuchFieldException ex) {
                badFields.add(values.getKey());
            }
        }
        if (!badFields.isEmpty()) {
            throwCLI("Unknown fields: " + badFields);
        }
    }

    private List<String> convertPDF(List<String> uris) {
        List<String> txtUris = new ArrayList<>();
        for (String inputFileURI : uris) {
            InfolisFile inputFile = dataStoreClient.get(InfolisFile.class, inputFileURI);
            if (null == inputFile) {
                throwCLI("File was not registered with the data store: " + inputFileURI);
            }
            if (null == inputFile.getMediaType()) {
                throwCLI("File has no mediaType: " + inputFileURI);
            }
            if (inputFile.getMediaType().startsWith("text/plain")) {
                txtUris.add(inputFileURI);
                continue;
            }
            // if the input file is a PDF file, convert it
            if (inputFile.getMediaType().startsWith("application/pdf")) {
                Execution convertExec = new Execution();
                convertExec.setAlgorithm(TextExtractorAlgorithm.class);
                convertExec.setOutputDirectory(textDir.toString());
                convertExec.setInputFiles(Arrays.asList(inputFile.getUri()));
                Algorithm algo = convertExec.instantiateAlgorithm(dataStoreClient, fileResolver);
                algo.run();
                txtUris.add(convertExec.getOutputFiles().get(0));
            }
        }
        return txtUris;
    }

    public List<String> postFiles(Path dir, String mimetype) {
        List<InfolisFile> infolisFiles = new ArrayList<>();
        DirectoryStream<Path> dirStream = null;
        try {
            dirStream = Files.newDirectoryStream(dir);
        } catch (IOException e) {
            throwCLI("Couldn't list directory contents of " + dir, e);
        }
        for (Path file : dirStream) {
            InfolisFile infolisFile = new InfolisFile();

            try (InputStream inputStream = Files.newInputStream(file)) {
                byte[] bytes = IOUtils.toByteArray(inputStream);
                infolisFile.setMd5(SerializationUtils.getHexMd5(bytes));
            } catch (IOException e) {
                throwCLI("Could not read file " + file, e);
            }

            infolisFile.setFileName(file.toString());
            infolisFile.setMediaType(mimetype);
            infolisFile.setFileStatus("AVAILABLE");
            infolisFiles.add(infolisFile);
        }
        return dataStoreClient.post(InfolisFile.class, infolisFiles);
    }

    private static void throwCLI(String msg) {
        throwCLI(msg, null);
    }

    private static void throwCLI(String msg, Exception e)
    {
        System.err.println("**ERROR** " + msg);
        if (null != e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }
        System.exit(1);
    }

    public void doMain(String args[]) throws FileNotFoundException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, IOException {
        CmdLineParser parser = new CmdLineParser(this, ParserProperties.defaults().withUsageWidth(120));
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("java " + getClass().getSimpleName() + " [options...]");
            parser.printUsage(System.err);
        }

        Files.createDirectories(dbDir);

        Execution exec = new Execution();
        try (Reader reader = Files.newBufferedReader(json, Charset.forName("UTF-8"))) {
            JsonObject jsonObject = Json.createReader(reader).readObject();
            checkJsonFile(jsonObject);
            try {
                setExecutionInputFiles(exec);
            } catch (IOException e) {
                throwCLI("Problem setting input files", e);
            }
            setExecutionFromJSON(jsonObject, exec);
        } catch (IOException e) {
            throwCLI("Problem reading JSON " + json, e);
        }
        doExecute(exec);
    }

    public static void main(String args[]) {
        try {
            new CommandLineExecuter().doMain(args);
        } catch (Exception e) {
            throwCLI("doMain", e);
        }
    }
}
