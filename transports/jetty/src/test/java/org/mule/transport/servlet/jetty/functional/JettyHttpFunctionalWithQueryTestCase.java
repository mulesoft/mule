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
import org.mule.api.transport.DispatchException;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class JettyHttpFunctionalWithQueryTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort port = new DynamicPort("port1");

    public JettyHttpFunctionalWithQueryTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "jetty-http-functional-test-with-query-service.xml"},
            {ConfigVariant.FLOW, "jetty-http-functional-test-with-query-flow.xml"}
        });
    }

    @Test
    public void testSend() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> props = new HashMap<String, Object>();
        MuleMessage result = client.send("clientEndpoint1", null, props);
        assertEquals("boobar", result.getPayloadAsString());
    }

    @Test
    public void testSendWithParams() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("foo", "noo");
        props.put("far", "nar");
        MuleMessage result = client.send("clientEndpoint2", null, props);
        assertEquals("noonar", result.getPayloadAsString());
    }

    @Test(expected = DispatchException.class)
    public void testSendWithBadParams() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("hoo", "noo");
        props.put("har", "nar");

        client.send("clientEndpoint2", null, props);
    }
}
