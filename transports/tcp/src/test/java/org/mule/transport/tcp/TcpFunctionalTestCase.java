/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

public class TcpFunctionalTestCase extends AbstractServiceAndFlowTestCase 
{

    protected static String TEST_MESSAGE = "Test TCP Request";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    public TcpFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
        
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "tcp-functional-test-service.xml"},
            {ConfigVariant.FLOW, "tcp-functional-test-flow.xml"}
        });
    }

    @Test
    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

    @Test
    public void testDispatchAndReply() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("asyncClientEndpoint", TEST_MESSAGE, null);
        // MULE-2754
        Thread.sleep(100);
        MuleMessage result =  client.request("asyncClientEndpoint", 10000);
        assertNotNull(result);
        assertEquals(TEST_MESSAGE + " Received Async", result.getPayloadAsString());
    }

    public void timeMultipleSend() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        long now = System.currentTimeMillis();
        int count = 1000;
        for (int i = 0; i < count; i++)
        {
            MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, null);
            assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
        }
        long later = System.currentTimeMillis();
        double speed = count * 1000.0 / (later - now);
        logger.error(speed + " messages per second");
    }

}
