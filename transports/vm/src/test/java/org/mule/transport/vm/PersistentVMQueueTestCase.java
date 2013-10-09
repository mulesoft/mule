/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

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
            {ConfigVariant.SERVICE, "vm/persistent-vmqueue-test-service.xml"},
            {ConfigVariant.FLOW, "vm/persistent-vmqueue-test-flow.xml"}});
    }

    @Test
    public void testAsynchronousDispatching() throws Exception
    {
        String input = "Test message";
        String[] output = {"Test", "message"};
        MuleClient client = new MuleClient(muleContext);
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
        MuleClient client = new MuleClient(muleContext);
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
