/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.ws.server.testws;

import io.github.infolis.model.TestConfig;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
//import org.json.JSONObject;

/**
 *
 * @author domi
 */
@Path("/test")
public class TestWebservice {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String message() {
        return "Nope!";
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public TestConfig PDFToText(TestConfig inputJsonObj) throws Exception {        
        inputJsonObj.setOutput("/out1/");
        PDF2Text.convert(inputJsonObj.getInput(), inputJsonObj.getOutput(), false);
        return inputJsonObj;
    }
}
