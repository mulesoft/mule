/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * This test was set for the new changes due to Mule1199
 */
public class CustomByteProtocolTestCase extends AbstractServiceAndFlowTestCase
{
    final private int messages = 100;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public CustomByteProtocolTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "custom-serialisation-mule-config-service.xml"},
            {ConfigVariant.FLOW, "custom-serialisation-mule-config-flow.xml"}
        });
    }            

    @Test
    public void testCustomObject() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        NonSerializableMessageObject message = new NonSerializableMessageObject(1, "Hello", true);

        for (int i = 0; i < messages; i++)
        {
            client.dispatch("vm://in", new DefaultMuleMessage(message, muleContext));
        }

        for (int i = 0; i < messages; i++)
        {
            MuleMessage msg = client.request("vm://out", 30000);
            assertTrue(msg.getPayload() instanceof NonSerializableMessageObject);
            NonSerializableMessageObject received = (NonSerializableMessageObject)msg.getPayload();
            assertEquals("Hello", received.s);
            assertEquals(1, received.i);
            assertEquals(true, received.b);
        }
    }

}