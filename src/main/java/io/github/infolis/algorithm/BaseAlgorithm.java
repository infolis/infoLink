package io.github.infolis.algorithm;

import io.github.infolis.datastore.AbstractClient;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.LocalClient;
import io.github.infolis.datastore.TempFileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.util.SerializationUtils;
import java.text.DateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kba
 * @author domi
 */
public abstract class BaseAlgorithm implements Algorithm {

    private static final Logger log = LoggerFactory.getLogger(BaseAlgorithm.class);
    /*
     * The list of algorithms
     */
    public static Map<String, Class<? extends BaseAlgorithm>> algorithms = new HashMap<>();

    static {
        algorithms.put(TextExtractorAlgorithm.class.getSimpleName(), TextExtractorAlgorithm.class);
    }

    public BaseAlgorithm(
            DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
            FileResolver inputFileResolver, FileResolver outputFileResolver) {
        this.outputDataStoreClient = outputDataStoreClient;
        this.inputDataStoreClient = inputDataStoreClient;
        this.outputFileResolver = outputFileResolver;
        this.inputFileResolver = inputFileResolver;
    }

    private Execution execution;
    private FileResolver outputFileResolver;
    private FileResolver inputFileResolver;
    private DataStoreClient inputDataStoreClient;
    private DataStoreClient outputDataStoreClient;

    public void baseValidate() throws IllegalAlgorithmArgumentException {
        if (null == getExecution()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "execution", "Algorithm must have a 'Excecution' set to run().");
        }
        if (null == getInputFileResolver()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "inputFileResolver", "Algorithm must have an input 'FileResolver' set to run().");
        }
        if (null == getOutputFileResolver()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "outputFileResolver", "Algorithm must have an output  'FileResolver' set to run().");
        }
        if (null == getOutputDataStoreClient()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "dataStoreClient", "Algorithm must have a 'dataStoreClient' set to run().");
        }
    }

    @Override
    public void debug(Logger log, String fmt, Object... args) {
        log(log, fmt, "DEBUG", args);
    }

    @Override
    public void fatal(Logger log, String fmt, Object... args) {
        log(log, fmt, "FATAL", args);
    }

    @Override
    public void error(Logger log, String fmt, Object... args) {
        log(log, fmt, "ERROR", args);
    }

    @Override
    public DataStoreClient getOutputDataStoreClient() {
        return outputDataStoreClient;
    }

    @Override
    public DataStoreClient getInputDataStoreClient() {
        return inputDataStoreClient;
    }

    @Override
    public Execution getExecution() {
        return execution;
    }

    @Override
    public FileResolver getInputFileResolver() {
        return inputFileResolver;
    }

    @Override
    public FileResolver getOutputFileResolver() {
        return outputFileResolver;
    }

    @Override
    public AbstractClient getTempDataStoreClient() {
        return new LocalClient(UUID.randomUUID());
    }

    @Override
    public TempFileResolver getTempFileResolver() {
        return new TempFileResolver();
    }

    @Override
    public void info(Logger log, String fmt, Object... args) {
        log(log, fmt, "INFO", args);
    }

    private String log(Logger log, String fmt, String level, Object... args) {
        final String str = String.format(fmt.replaceAll("\\{\\}", "%s"), args);
        switch (level.toLowerCase()) {
            case "trace":
                log.trace(str);
                break;
            case "debug":
                log.debug(str);
                break;
            case "info":
                log.info(str);
                break;
            case "warn":
                log.warn(str);
                break;
            case "error":
                log.error(str);
                break;
            case "fatal":
                log.error(str);
                break;
        }
        getExecution().getLog().add(String.format("%s [%s -- %s] %s", level, new Date(), getClass().getSimpleName(), str));
        return str;
    }

    protected void persistExecution() throws BadRequestException {
        log.debug("Persisting execution");
        if (null != getExecution().getUri()) {
            getOutputDataStoreClient().put(Execution.class, getExecution());
        } else {
            getOutputDataStoreClient().post(Execution.class, getExecution());
        }
    }

    @Override
    public final void run() {
        log.debug("{}", SerializationUtils.toJSON(getExecution()));
        try {
            baseValidate();
            validate();
        } catch (IllegalAlgorithmArgumentException | RuntimeException e) {
            getExecution().setStatus(ExecutionStatus.FAILED);
            getExecution().getLog().add(e.getMessage());
            getExecution().setEndTime(new Date());
            return;
        } finally {
            persistExecution();
        }
        getExecution().setStatus(ExecutionStatus.STARTED);
        getExecution().setStartTime(new Date());
        try {
            execute();
        } catch (Exception e) {
            log.error("Execution threw an Exception: {}", e);
            getExecution().setStatus(ExecutionStatus.FAILED);
            getExecution().setEndTime(new Date());
        } finally {
            persistExecution();
        }
    }

    @Override
    public void setDataStoreClient(DataStoreClient dataStoreClient) {
        this.inputDataStoreClient = dataStoreClient;
    }

    @Override
    public void setExecution(Execution execution) {
        this.execution = execution;
    }

    /**
     * Update the current process of the execution each second.
     * 
     * @param done
     * @param total 
     */
    @Override
    public void updateProgress(int done, int total) {
        Date now = new Date(System.currentTimeMillis());
        if ((now.getTime() - getExecution().getStartTime().getTime()) > 1000) {            
            int percentage = new Double(100*((double)done/(double)total)).intValue();
            getExecution().setProgress(percentage);
            if (null != getExecution().getUri()) {
                getOutputDataStoreClient().put(Execution.class, getExecution());
            } else {
                getOutputDataStoreClient().post(Execution.class, getExecution());
            }
        }
    }
}
