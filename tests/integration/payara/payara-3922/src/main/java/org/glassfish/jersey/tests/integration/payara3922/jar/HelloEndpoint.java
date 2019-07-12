// Copyright [2019] [Payara Foundation and/or its affiliates]
package org.glassfish.jersey.tests.integration.payara3922.jar;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

public interface HelloEndpoint {

    @POST
    @Path("/logHello")
    void sayHelloToLog();

}
