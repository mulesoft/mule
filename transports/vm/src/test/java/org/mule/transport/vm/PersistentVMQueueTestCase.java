/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class PersistentVMQueueTestCase extends AbstractServiceAndFlowTestCase
{
    private static final int RECEIVE_TIMEOUT = 5000;

    public PersistentVMQueueTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.FLOW, "vm/persistent-vmqueue-test-flow.xml"}});
    }

    @Test
    public void testAsynchronousDispatching() throws Exception
    {
        String input = "Test message";
        String[] output = {"Test", "message"};

        MuleClient client = muleContext.getClient();
        client.dispatch("vm://receiver", input, null);
        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertNotNull(result.getPayload());
        assertNull(result.getExceptionPayload());
        String[] payload = (String[]) result.getPayload();
        assertEquals(output.length, payload.length);
        for (int i = 0; i < output.length; i++)
        {
            assertEquals(output[i], payload[i]);
        }
    }

    @Test
    public void testAsynchronousDispatchingInFlow() throws Exception
    {
        String input = "Test message";
        String[] output = {"Test", "message"};

        MuleClient client = muleContext.getClient();
        client.dispatch("vm://flowReceiver", input, null);
        MuleMessage result = client.request("vm://flowOut", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertNotNull(result.getPayload());
        assertNull(result.getExceptionPayload());
        String[] payload = (String[]) result.getPayload();
        assertEquals(output.length, payload.length);
        for (int i = 0; i < output.length; i++)
        {
            assertEquals(output[i], payload[i]);
        }
    }
}
