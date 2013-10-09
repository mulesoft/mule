/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.FlowAssert;
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
    protected String getConfigResources()
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
        MuleClient client = new MuleClient(muleContext);
        HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put("Content-Type","application/x-www-form-urlencoded");

        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/resources/client", "name=John&lastname=Galt", properties);

        assertNotNull(response);
        assertEquals("client", response.getInboundProperty("http.relative.path"));
        assertEquals("http://localhost:" + dynamicPort.getNumber() + "/resources", response.getInboundProperty("http.context.uri"));
        assertEquals("Storing client with name = John and lastname = Galt", response.getPayloadAsString());
    }

    @Test
    public void testRedirectionWithRelativeProperty() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/redirect/products?retrieve=all&order=desc", TEST_MESSAGE, null);
        assertEquals("Successfully redirected: products?retrieve=all&order=desc", response.getPayloadAsString());
    }

}
