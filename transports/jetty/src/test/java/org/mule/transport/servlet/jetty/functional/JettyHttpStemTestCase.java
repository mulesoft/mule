/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;


public class JettyHttpStemTestCase extends AbstractServiceAndFlowTestCase
{

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");
    
    public JettyHttpStemTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "jetty-http-stem-test-service.xml"},
            {ConfigVariant.FLOW, "jetty-http-stem-test-flow.xml"}
        });
    }  
    
    @Test
    public void testStemMatchingHttp() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        doTest(client, "http://localhost:"+ dynamicPort1.getNumber() +"/foo", "Hello World");
        doTest(client, "http://localhost:"+ dynamicPort1.getNumber() +"/foo/bar", "Hello World");
        doTest(client, "http://localhost:"+ dynamicPort1.getNumber() +"/foo/bestmatch", "Hello World Best Match");
    }

    protected void doTest(MuleClient client, String url, String value) throws Exception
    {
        MuleMessage result = client.send(url, "Hello", null);
        assertEquals(value, result.getPayloadAsString());
        final int status = result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0);
        assertEquals(200, status);
    }
}
