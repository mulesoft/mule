/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class TcpEchoDirectProtocolTestCase extends AbstractServiceAndFlowTestCase
{
    protected static String TEST_MESSAGE = "Test TCP Request";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public TcpEchoDirectProtocolTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "tcp-echo-test-service.xml"},
            {ConfigVariant.FLOW, "tcp-echo-test-flow.xml"}
        });
    }

    @Test
    public void testSend() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage response = client.send(((InboundEndpoint) muleContext.getRegistry().lookupObject("inBounceTcpMMP")).getAddress(),
            TEST_MESSAGE, null);

        assertNotNull(response);
        assertEquals(TEST_MESSAGE, response.getPayload());
    }
}
