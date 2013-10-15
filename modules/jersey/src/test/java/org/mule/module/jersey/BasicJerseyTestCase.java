/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BasicJerseyTestCase extends FunctionalTestCase
{
    
    @Override
    protected String getConfigResources()
    {
        return "basic-conf.xml";
    }

    @Test
    public void testBasic() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage result = client.send("http://localhost:63081/helloworld", "", null);
        assertEquals((Integer)200, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertEquals("Hello World", result.getPayloadAsString());

        // try invalid url
        result = client.send("http://localhost:63081/hello", "", null);
        assertEquals((Integer)404, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));

        Map<String, String> props = new HashMap<String, String>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);
        result = client.send("http://localhost:63081/helloworld", "", props);
        assertEquals((Integer)405, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));

        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_DELETE);
        result = client.send("http://localhost:63081/helloworld", "", props);
        assertEquals("Hello World Delete", result.getPayloadAsString());
        assertEquals((Integer)200, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
    }

    @Test
    public void testParams() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        Map<String, String> props = new HashMap<String, String>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);
        MuleMessage result = client.send("http://localhost:63081/helloworld/sayHelloWithUri/Dan", "", props);
        assertEquals((Integer)200, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertEquals("Hello Dan", result.getPayloadAsString());


        result = client.send("http://localhost:63081/helloworld/sayHelloWithJson/Dan", "", props);
        assertEquals((Integer)200, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertEquals("{\"message\":\"Hello Dan\"}", result.getPayloadAsString());

        result = client.send("http://localhost:63081/helloworld/sayHelloWithQuery?name=Dan", "", props);
        assertEquals((Integer)200, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertEquals("Hello Dan", result.getPayloadAsString());

        props.put("X-Name", "Dan");
        result = client.send("http://localhost:63081/helloworld/sayHelloWithHeader", "", props);
        assertEquals((Integer)201, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertEquals("Hello Dan", result.getPayloadAsString());
        assertEquals("Dan", result.getInboundProperty("X-ResponseName"));
    }

    @Test
    public void testThrowException() throws Exception
    {
    	callThrowException(500, "");
    }

    protected void callThrowException(Integer expectedErrorCode, String expectedData) throws Exception
    {
    	MuleClient client = new MuleClient(muleContext);

    	Map<String, String> props = new HashMap<String, String>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);
        MuleMessage result = client.send("http://localhost:63081/helloworld/throwException", "", props);
        assertEquals(expectedErrorCode, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertEquals(expectedData, result.getPayloadAsString());
    }

}
