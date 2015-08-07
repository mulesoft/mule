/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;

public class HttpPropertiesTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "http-properties-conf.xml";
    }

    @Test
    public void testPropertiesGetMethod() throws Exception
    {
        GetMethod httpGet = new GetMethod("http://localhost:" + dynamicPort.getNumber() + "/resources/client?id=1");
        new HttpClient().executeMethod(httpGet);
        String result =  httpGet.getResponseBodyAsString();
        assertEquals("Retrieving client with id = 1", result);
    }

    @Test
    public void testPropertiesPostMethod() throws Exception
    {
        MuleClient client = muleContext.getClient();
        HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put("Content-Type","application/x-www-form-urlencoded");

        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/resources/client", new DefaultMuleMessage("name=John&lastname=Galt", properties, muleContext));

        assertNotNull(response);
        assertEquals("client", response.getInboundProperty("http.relative.path"));
        assertEquals("http://localhost:" + dynamicPort.getNumber() + "/resources", response.getInboundProperty("http.context.uri"));
        assertEquals("Storing client with name = John and lastname = Galt", response.getPayloadAsString());
    }

    @Test
    public void testRedirectionWithRelativeProperty() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/redirect/products?retrieve=all&order=desc", getTestMuleMessage(TEST_MESSAGE));
        assertEquals("Successfully redirected: products?retrieve=all&order=desc", response.getPayloadAsString());
    }
}
