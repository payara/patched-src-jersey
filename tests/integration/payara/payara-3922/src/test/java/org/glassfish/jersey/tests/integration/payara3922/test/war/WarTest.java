// Copyright [2019] [Payara Foundation and/or its affiliates]
package org.glassfish.jersey.tests.integration.payara3922.test.war;

import javax.ejb.EJB;

import org.glassfish.jersey.tests.integration.payara3922.jar.HelloEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author David Matějček
 */
@RunWith(Arquillian.class)
public class WarTest {

    @EJB
    private HelloEndpoint bean;


    /**
     * Initializes the deployment unit.
     *
     * @return {@link WebArchive} to deploy to the container.
     * @throws Exception exception
     */
    @Deployment
    public static WebArchive getArchiveToDeploy() throws Exception {
        final JavaArchive library = ShrinkWrap.create(JavaArchive.class) //
            .addPackages(true, "org.glassfish.jersey.tests.integration.payara3922.jar");

        final WebArchive module = ShrinkWrap.create(WebArchive.class) //
            .addPackages(true, "org.glassfish.jersey.tests.integration.payara3922.war") //
            .addAsLibraries(library) //
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml"); //

        System.out.println(module.toString(true));
        return module;
    }


    @Test
    public void test() {
        this.bean.sayHelloToLog();
    }

}
