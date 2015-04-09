/*
 * JAX-RS Application, wiring up the REST interface to the backend.
 */
package io.github.infolis.ws;

import io.github.infolis.ws.backend.Backend;
import io.github.infolis.ws.testws.Test;
import io.github.infolis.ws.testws.Upload;

import java.io.IOException;
import java.util.Set;

import javax.ws.rs.core.Application;

/**
 *
 * @author domi
 * @author kba
 */
@javax.ws.rs.ApplicationPath("infolis-api")
public class InfolisApplication extends Application {
	
	public InfolisApplication() throws IOException {
		super();
		
		// Make sure the configuration is loaded and valid
		InfolisApplicationConfig.validate();
	}

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();
        resources.add(Backend.class);
        resources.add(Test.class);
        resources.add(Upload.class);
        return resources;
    }

}
