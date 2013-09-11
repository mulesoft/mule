/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpDynamicFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    protected static String TEST_REQUEST = "Test Http Request";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    public HttpDynamicFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-dynamic-functional-test-flow.xml"},
            {ConfigVariant.FLOW, "http-dynamic-functional-test-service.xml"}
        });
    }

    @Test
    public void testSend() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("port", dynamicPort1.getNumber());
        props.put("path", "foo");

        MuleMessage result = client.send("clientEndpoint", TEST_REQUEST, props);
        assertEquals(TEST_REQUEST + " Received 1", result.getPayloadAsString());

        props.put("port", dynamicPort2.getNumber());
        result = client.send("clientEndpoint", TEST_REQUEST, props);
        assertEquals(TEST_REQUEST + " Received 2", result.getPayloadAsString());
    }
}
