package io.github.infolis.ws.server;

import io.github.infolis.algorithm.Algorithm;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.FileResolverFactory;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.scheduler.ExecutionScheduler;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web service for executing algorithms.
 *
 * @author kba
 */
@Path("/executor")
public class ExecutorWebservice {

	Logger log = LoggerFactory.getLogger(ExecutorWebservice.class);

	private DataStoreClient dataStoreClient = DataStoreClientFactory.create(DataStoreStrategy.CENTRAL);
	private FileResolver fileResolver = FileResolverFactory.create(DataStoreStrategy.CENTRAL);

	@GET
	@Produces("application/json")
	public List<String> getExecutionStatus(@QueryParam("status") String statusParam) {
	    ExecutionScheduler executionPool = ExecutionScheduler.getInstance();
	    List<String> executionURIs = new ArrayList<>();
	    ExecutionStatus status = null;
	    try {
	        status = ExecutionStatus.valueOf(statusParam);
	        executionURIs.addAll(executionPool.getByStatus(status));
	    } catch (IllegalArgumentException | NullPointerException e) {
	        executionURIs.addAll(executionPool.getAllExcecutions());
	    }
	    return executionURIs;
	}

	@POST
	@Produces("application/json")
	public Response startExecution(@QueryParam("id") String executionUri) {
		ResponseBuilder resp = Response.ok();

		Execution execution;
		try {
			execution = dataStoreClient.get(Execution.class, executionUri);
			if (null == execution) {
				throw new ProcessingException("Could not find execution " + executionUri);
			}
		} catch (BadRequestException | ProcessingException e1) {
			e1.printStackTrace();
			String msg = "Could not retrieve execution " + executionUri;
			resp.status(404);
			resp.entity(msg + "\n" + e1.getMessage());
			return resp.build();
		}
		Class<? extends Algorithm> algoClass = execution.getAlgorithm();
		Algorithm algo = null;
		if (algoClass == null) {
			String msg = "ERROR: No algorithm provided for execution";
			execution.getLog().add(msg);
			execution.setStatus(ExecutionStatus.FAILED);
			resp.entity(msg);
			resp.status(404);
		} else {
			try {
				algo = execution.instantiateAlgorithm(dataStoreClient, fileResolver);
			} catch (RuntimeException e) {
				String msg = "ERROR: Could not instantiate algorithm " + algoClass.getName();
				execution.getLog().add(msg);
				execution.setStatus(ExecutionStatus.FAILED);
				resp.entity(msg);
				resp.status(400);
			}
		}
		if (algo == null) {
			String msg = "ERROR: Algo is still null for " + algoClass.getName();
			execution.getLog().add(msg);
			execution.setStatus(ExecutionStatus.FAILED);
			resp.entity(msg);
			resp.status(400);
		} else {
			dataStoreClient.put(Execution.class, execution);
                        ExecutionScheduler exe = ExecutionScheduler.getInstance();
                        exe.execute(algo);
		}
		return resp.build();
	}
        
	@DELETE
	@Produces("application/json")
	public Response stopExecution(@QueryParam("id") String executionUri) {
            ExecutionScheduler exe = ExecutionScheduler.getInstance();
            exe.stopExecution(executionUri);
	    log.info("Received DELETE request for " + executionUri + " in thread " + exe.futureList.get(executionUri));
            ResponseBuilder resp = Response.ok();
            return resp.build();
        }
}
