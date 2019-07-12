// Copyright [2019] [Payara Foundation and/or its affiliates]
package org.glassfish.jersey.tests.integration.payara3922.war;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/rs")
public class HelloRestResourceConfig extends ResourceConfig {

    public HelloRestResourceConfig() {
        packages(HelloRestResourceConfig.class.getPackage().getName());
    }
}
