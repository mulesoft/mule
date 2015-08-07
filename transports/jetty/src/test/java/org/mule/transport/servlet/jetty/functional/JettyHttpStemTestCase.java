/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty.functional;

import static org.junit.Assert.assertEquals;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;

import org.junit.Rule;
import org.junit.Test;


public class JettyHttpStemTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Override
    protected String getConfigFile()
    {
        return "jetty-http-stem-test-flow.xml";
    }

    @Test
    public void testStemMatchingHttp() throws Exception
    {
        MuleClient client = muleContext.getClient();
        doTest(client, "http://localhost:"+ dynamicPort1.getNumber() +"/foo", "Hello World");
        doTest(client, "http://localhost:"+ dynamicPort1.getNumber() +"/foo/bar", "Hello World");
        doTest(client, "http://localhost:"+ dynamicPort1.getNumber() +"/foo/bestmatch", "Hello World Best Match");
    }

    protected void doTest(MuleClient client, String url, String value) throws Exception
    {
        MuleMessage result = client.send(url, getTestMuleMessage("Hello"), newOptions().method(POST.name()).build());
        assertEquals(value, result.getPayloadAsString());
        final int status = result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0);
        assertEquals(200, status);
    }
}
