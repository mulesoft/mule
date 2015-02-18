/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.endpoints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.client.DefaultLocalMuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class DynamicEndpointWithAsyncResponseTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE,
            "org/mule/test/integration/endpoints/dynamic-endpoint-with-async-response-config.xml"}

        });
    }

    public DynamicEndpointWithAsyncResponseTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testDynamicEndpointWithAsyncResponse() throws Exception
    {
        MuleMessage message = getTestMuleMessage("hello");
        message.setOutboundProperty("host", "localhost");
        message.setOutboundProperty("port", port1.getNumber());
        message.setOutboundProperty("path", "/TEST");

        DefaultLocalMuleClient client = new DefaultLocalMuleClient(muleContext);
        MuleMessage response = client.send("vm://vmProxy", message);
        assertEquals("hello Received", response.getPayloadAsString());

        response = client.request("vm://vmOut", 5000);
        assertNotNull(response);
    }
}
