/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Simple synch test used to study message flow.
 */
public class VMSynchTestCase extends AbstractServiceAndFlowTestCase
{
    public VMSynchTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "vm/vm-synch-test-service.xml"},
            {ConfigVariant.FLOW, "vm/vm-synch-test-flow.xml"}
        });
    }

    @Test
    public void testSingleMessage() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response =  client.send("vm://bridge", "Message", null);
        assertNotNull("Response is null", response);
        assertEquals("Message Received", response.getPayload());
    }

    @Test
    public void testManyMessage() throws Exception
    {
        for (int i = 0; i < 1000; i++)
        {
            testSingleMessage();
        }
    }
}
