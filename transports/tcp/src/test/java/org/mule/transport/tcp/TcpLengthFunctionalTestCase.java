/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class TcpLengthFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    protected static String TEST_MESSAGE = "Test TCP Request";
    private int timeout = 60 * 1000 / 20;

    @ClassRule
    public static DynamicPort dynamicPort1 = new DynamicPort("port1");

    @ClassRule
    public static DynamicPort dynamicPort2 = new DynamicPort("port2");

    @ClassRule
    public static DynamicPort dynamicPort3 = new DynamicPort("port3");

    public TcpLengthFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setDisposeContextPerClass(true);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "tcp-length-functional-test-service.xml"},
            {ConfigVariant.FLOW, "tcp-length-functional-test-flow.xml"}
        });
    }

    @Test
    public void testSend() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

    @Test
    public void testDispatchAndReplyViaStream() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("asyncClientEndpoint1", TEST_MESSAGE, null);
        // MULE-2754
        Thread.sleep(200);
        MuleMessage result =  client.request("asyncClientEndpoint1", timeout);
        // expect failure - streaming not supported
        assertNull(result);
    }

    @Test
    public void testDispatchAndReply() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("asyncClientEndpoint2", TEST_MESSAGE, null);
        // MULE-2754
        Thread.sleep(200);
        MuleMessage result =  client.request("asyncClientEndpoint2", timeout);
        // expect failure - TCP simply can't work like this
        assertNull(result);
    }
}
