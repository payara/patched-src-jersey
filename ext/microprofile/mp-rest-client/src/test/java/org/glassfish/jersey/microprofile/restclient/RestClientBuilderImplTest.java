package org.glassfish.jersey.microprofile.restclient;

import org.junit.Assert;
import org.junit.Test;

import static org.glassfish.jersey.microprofile.restclient.RestClientBuilderImpl.createProxyString;

public class RestClientBuilderImplTest {

    @Test
    public void createProxyStringTest() {
        Assert.assertTrue(createProxyString("localhost", 8765).equals("http://localhost:8765"));
        Assert.assertTrue(createProxyString("http://localhost", 8765).equals("http://localhost:8765"));
        Assert.assertTrue(createProxyString("127.0.0.1", 8765).equals("http://127.0.0.1:8765"));
        Assert.assertTrue(createProxyString("http://192.168.1.1", 8765).equals("http://192.168.1.1:8765"));
    }
}
