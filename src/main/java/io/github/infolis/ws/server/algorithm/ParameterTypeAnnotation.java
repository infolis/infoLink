/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.ws.server.algorithm;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 *
 * @author domi
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ParameterTypeAnnotation {
    public String type();
}
