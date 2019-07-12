// Copyright [2019] [Payara Foundation and/or its affiliates]
package org.glassfish.jersey.tests.integration.payara3922.war;

import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.tests.integration.payara3922.jar.HelloEndpoint;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@Path("hello")
public class HelloBean implements HelloEndpoint {

    @Override
    public void sayHelloToLog() {
        System.out.println("HELLO!");
    }
}
