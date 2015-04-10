/*
 * JAX-RS Application, wiring up the REST interface to the backend.
 */
package io.github.infolis.ws.server;

import io.github.infolis.ws.server.testws.TestWebservice;
import io.github.infolis.ws.server.testws.UploadWebservice;

import java.util.Set;

import javax.ws.rs.core.Application;

/**
 *
 * @author domi
 * @author kba
 */
@javax.ws.rs.ApplicationPath("infolis-api")
public class InfolisApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();
        resources.add(Backend.class);
        resources.add(TestWebservice.class);
        resources.add(UploadWebservice.class);
        return resources;
    }

}
