// Copyright [2019] [Payara Foundation and/or its affiliates]
package org.glassfish.jersey.tests.integration.payara3922.duplicit1;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/duplicit")
public class RestResourceConfigD1 extends ResourceConfig {

    public RestResourceConfigD1() {
        packages(RestResourceConfigD1.class.getPackage().getName());
        setClassLoader(Thread.currentThread().getContextClassLoader());
    }
}
