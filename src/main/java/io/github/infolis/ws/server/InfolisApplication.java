package io.github.infolis.ws.server;

import java.util.Set;

import javax.ws.rs.core.Application;

/**
 * JAX-RS Application, wiring up the REST interface to the backend.
 *
 * @author domi
 * @author kba
 */
@javax.ws.rs.ApplicationPath("infolis-api")
public class InfolisApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();
        resources.add(ExecutorWebservice.class);
        resources.add(UploadWebservice.class);
        return resources;
    }

}
