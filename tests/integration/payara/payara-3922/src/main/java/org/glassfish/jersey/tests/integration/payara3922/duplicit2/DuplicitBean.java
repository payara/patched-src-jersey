// Copyright [2019] [Payara Foundation and/or its affiliates]
package org.glassfish.jersey.tests.integration.payara3922.duplicit2;

import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Consumes(MediaType.TEXT_PLAIN)
@Produces(MediaType.APPLICATION_JSON)
@Stateless(name = "DuplicitBean2")
@Path("dup")
public class DuplicitBean {

    @GET
    @Path("ok")
    public String ok() {
        return "OK2";
    }
}
