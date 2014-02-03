/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class BasicJerseyTestCase extends AbstractServiceAndFlowTestCase
{
    public BasicJerseyTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Rule
    public DynamicPort port = new DynamicPort("port");

    private String URL = "http://localhost:" + port.getNumber() + "%s";

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "basic-conf.xml"},
        });
    }

    @Test
    public void testBasic() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.send(String.format(URL, "/helloworld"), "", null);
        assertEquals((Integer)200, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertEquals("Hello World", result.getPayloadAsString());

        // try invalid url
        result = client.send(String.format(URL, "/hello"), "", null);
        assertEquals((Integer)404, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);
        result = client.send(String.format(URL, "/helloworld"), "", props);
        assertEquals((Integer)405, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));

        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_DELETE);
        result = client.send(String.format(URL, "/helloworld"), "", props);
        assertEquals("Hello World Delete", result.getPayloadAsString());
        assertEquals((Integer)200, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
    }

    @Test
    public void testParams() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);
        MuleMessage result = client.send(String.format(URL, "/helloworld/sayHelloWithUri/Dan"), "", props);
        assertEquals((Integer)200, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertEquals("Hello Dan", result.getPayloadAsString());


        result = client.send(String.format(URL, "/helloworld/sayHelloWithJson/Dan"), "", props);
        assertEquals((Integer)200, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertEquals(getJsonHelloBean(), result.getPayloadAsString());

        result = client.send(String.format(URL, "/helloworld/sayHelloWithQuery?name=Dan"), "", props);
        assertEquals((Integer)200, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertEquals("Hello Dan", result.getPayloadAsString());

        props.put("X-Name", "Dan");
        result = client.send(String.format(URL, "/helloworld/sayHelloWithHeader"), "", props);
        assertEquals((Integer)201, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertEquals("Hello Dan", result.getPayloadAsString());
        assertEquals("Dan", result.getInboundProperty("X-ResponseName"));
    }

    @Test
    public void testThrowException() throws Exception
    {
        callThrowException(500, "Failed to invoke JerseyResourcesComponent{helloWorldResource.component}. Component that caused exception is: JerseyResourcesComponent{helloWorldResource.component}. Message payload is of type: String");
    }

    protected void callThrowException(Integer expectedErrorCode, String expectedData) throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);
        MuleMessage result = client.send(String.format(URL, "/helloworld/throwException"), "", props);
        assertEquals(expectedErrorCode, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertTrue(result.getPayloadAsString().contains(expectedData));
    }

    protected String getJsonHelloBean()
    {
        return "{\"message\":\"Hello Dan\",\"number\":\"0\"}";
    }
}
