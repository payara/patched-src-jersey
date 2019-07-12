// Copyright [2019] [Payara Foundation and/or its affiliates]
package org.glassfish.jersey.tests.integration.payara3922.test.war;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

/**
 * @author David Matějček
 */
@RunWith(Arquillian.class)
public class EarTest {

    private static final int httpPort = Integer.getInteger("payara.port.http");

    @Rule
    public final TestName name = new TestName();

    private Client client;
    private WebTarget targetBase;


    /**
     * Only to mark the class initialization in logs
     */
    @BeforeClass
    public static void initContainer() {
        System.err.println("initContainer()");
    }


    @Before
    public void before() {
        System.err.println("before(). Test name: " + this.name.getMethodName());
        this.client = ClientBuilder.newClient();
        this.targetBase = this.client.target("http://localhost:" + httpPort);
    }


    @After
    public void after() {
        System.err.println("after(). Test name: " + this.name.getMethodName());
        this.client.close();
    }


    /**
     * Initializes the deployment unit.
     *
     * @return {@link EnterpriseArchive} to deploy to the container.
     * @throws Exception exception
     */
    @Deployment(testable = false)
    public static EnterpriseArchive getArchiveToDeploy() throws Exception {

        final JavaArchive library = ShrinkWrap.create(JavaArchive.class) //
            .addPackages(true, "org.glassfish.jersey.tests.integration.payara3922.jar");

        final WebArchive war = ShrinkWrap.create(WebArchive.class, "payara-3922.war") //
            .addPackages(true, "org.glassfish.jersey.tests.integration.payara3922.war") //
            .addAsLibraries(library) //
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml"); //
        System.out.println("WAR: \n" + war.toString(true));

        final WebArchive dupl1 = ShrinkWrap.create(WebArchive.class, "payara-3121-1.war") //
            .addPackages(true, "org.glassfish.jersey.tests.integration.payara3922.duplicit1") //
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml"); //
        System.out.println("WAR-dupl1: \n" + dupl1.toString(true));

        final WebArchive dupl2 = ShrinkWrap.create(WebArchive.class, "payara-3121-2.war") //
            .addPackages(true, "org.glassfish.jersey.tests.integration.payara3922.duplicit2") //
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml"); //
        System.out.println("WAR-dupl2: \n" + dupl2.toString(true));

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "payara-3922.ear") //
            .addAsModule(war) //
            .addAsModule(dupl1) //
            .addAsModule(dupl2) //
            .setApplicationXML("application.xml");
        System.out.println("EAR: \n" + ear.toString(true));
        return ear;
    }


    @Test
    public void testInterfaceWithNoLocalOrRemoteAnnotation() throws Exception {
        final WebTarget webTarget = this.targetBase.path("/rs/hello/logHello");
        final Response response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.json(""));
        assertNotNull("response", response);
        assertEquals("response.status", 204, response.getStatus());
    }


    @Test
    @Ignore("Fails because of different classloading rules, but produced ear still can be tested manually.")
    public void testDuplicitPaths() throws Exception {
        final WebTarget t1 = this.targetBase.path("/d1/duplicit/dup/ok");
        final String responseT1 = t1.request(MediaType.APPLICATION_JSON).get(String.class);
        assertEquals("responseT1", "OK1", responseT1);

        final WebTarget t2 = this.targetBase.path("/d2/duplicit/dup/ok");
        final String responseT2 = t2.request(MediaType.APPLICATION_JSON).get(String.class);
        assertEquals("responseT2", "OK2", responseT2);
    }

}
