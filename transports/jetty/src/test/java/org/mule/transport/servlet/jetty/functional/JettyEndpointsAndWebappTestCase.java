/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty.functional;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.servlet.jetty.AbstractWebappsTestCase;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;

public class JettyEndpointsAndWebappTestCase extends AbstractWebappsTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Rule
    public DynamicPort port2 = new DynamicPort("port2");

    @Rule
    public DynamicPort port3 = new DynamicPort("port3");

    @Override
    protected String getConfigFile()
    {
        return "jetty-endpoints-and-webapp-test.xml";
    }

    @Test
    public void listensInWebappsPort() throws Exception
    {
        sendRequestAndAssertCorrectResponse(String.format("http://localhost:%d/%s", port1.getNumber(), WEBAPP_TEST_URL));
    }

    @Test
    public void listensInEndpointsPorts() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send(String.format("http://localhost:%d/contextA", port2.getNumber()), TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE, response.getPayloadAsString());
        response = client.send(String.format("http://localhost:%d/contextB", port3.getNumber()), TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE, response.getPayloadAsString());
    }

    @Test
    public void doesNotListenWebappInEndpointPorts() throws Exception
    {
        assertNotFound(String.format("http://localhost:%d/%s", port2.getNumber(), WEBAPP_TEST_URL));
        assertNotFound(String.format("http://localhost:%d/%s", port3.getNumber(), WEBAPP_TEST_URL));
    }

    @Test
    public void doesNotListenEndpointInWebappsPort() throws Exception
    {
        assertNotFound(String.format("http://localhost:%d/contextA", port1.getNumber()));
        assertNotFound(String.format("http://localhost:%d/contextB", port1.getNumber()));
    }

    @Test
    public void doesNotListenEndpointInDifferentEndpointPort() throws Exception
    {
        assertNotFound(String.format("http://localhost:%d/contextA", port3.getNumber()));
        assertNotFound(String.format("http://localhost:%d/contextB", port2.getNumber()));
    }

    private void assertNotFound(String url) throws IOException
    {
        GetMethod method = new GetMethod(url);
        int statusCode = new HttpClient().executeMethod(method);
        assertEquals(HttpStatus.SC_NOT_FOUND, statusCode);
    }

}
