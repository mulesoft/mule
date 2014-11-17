/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
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
        MuleClient client = muleContext.getClient();
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