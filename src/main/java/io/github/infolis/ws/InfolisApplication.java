/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.ws;

import io.github.infolis.ws.backend.Backend;
import io.github.infolis.ws.testws.Test;

import java.util.Set;

import javax.ws.rs.core.Application;

/**
 *
 * @author domi
 */
@javax.ws.rs.ApplicationPath("webresources")
public class InfolisApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();
        addRestResourceClasses(resources);
        return resources;
    }

    /**
     * Do not modify addRestResourceClasses() method. It is automatically
     * populated with all resources defined in the project. If required, comment
     * out calling this method in getClasses().
     */
    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(Backend.class);
        resources.add(Test.class);
    }

}
