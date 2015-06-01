package io.github.infolis.ws.server;

import io.github.infolis.algorithm.Algorithm;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.FileResolverFactory;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;

import java.net.URI;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
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
	
	@POST
	public Response startExecution(@QueryParam("id") String executionUri) {
		ResponseBuilder resp = Response.ok();

		Execution execution;
		try {
			execution = dataStoreClient.get(Execution.class, URI.create(executionUri));
			if (null == execution) {
				throw new ProcessingException("Could not find execution " + executionUri);
			}
		} catch (BadRequestException | ProcessingException e1) {
			e1.printStackTrace();
			String msg = "Could not retrieve execution " + executionUri;
			resp.status(404);
			resp.entity(msg);
			return resp.build();
		}
		Class<? extends Algorithm> algoClass = execution.getAlgorithm();
		Algorithm algo = null;
		if (null == algoClass) {
			String msg = "ERROR: No such algorithm: " + algoClass;
			execution.getLog().add(msg);
			execution.setStatus(ExecutionStatus.FAILED);
			resp.entity(msg);
			resp.status(404);
		} else {
			try {
				algo = algoClass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				String msg = "ERROR: Could not instantiate algorithm " + algoClass.getName();
				execution.getLog().add(msg);
				execution.setStatus(ExecutionStatus.FAILED);
				resp.entity(msg);
				resp.status(400);
			}
		}
		if (algo != null) {
			algo.setExecution(execution);
			algo.setDataStoreClient(dataStoreClient);
			algo.setFileResolver(fileResolver);
			dataStoreClient.put(Execution.class, execution);
			new Thread(algo).start();
		} else {
			String msg = "ERROR: Algo is still null for " + algoClass.getName();
			execution.getLog().add(msg);
			execution.setStatus(ExecutionStatus.FAILED);
			resp.entity(msg);
			resp.status(400);
		}
		return resp.build();
	}
}
