// Copyright [2019] [Payara Foundation and/or its affiliates]
package org.glassfish.jersey.tests.integration.payara3922.duplicit2;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/duplicit")
public class RestResourceConfigD2 extends ResourceConfig {

    public RestResourceConfigD2() {
        packages(RestResourceConfigD2.class.getPackage().getName());
    }

}
