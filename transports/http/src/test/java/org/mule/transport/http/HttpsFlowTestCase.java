/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HttpsFlowTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "https-flow-config.xml";
    }

    @Test
    public void testSecureFlow() throws Exception
    {
        String url = String.format("https://localhost:%1d/?message=Hello", dynamicPort.getNumber());

        GetMethod method = new GetMethod(url);
        HttpClient client = new HttpClient();

        int responseCode = client.executeMethod(method);
        assertEquals(HttpConstants.SC_OK, responseCode);

        String result = method.getResponseBodyAsString();
        assertEquals("/?message=Hello received", result);
    }
}


