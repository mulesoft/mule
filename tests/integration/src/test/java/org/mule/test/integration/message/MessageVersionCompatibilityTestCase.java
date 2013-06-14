/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.message;

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

/**
 * Test case for EE-1820
 */
public class MessageVersionCompatibilityTestCase extends AbstractServiceAndFlowTestCase
{
    private final int TIMEOUT = 5000;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/messaging/message-version-compatibility-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/messaging/message-version-compatibility-flow.xml"}
        });
    }

    public MessageVersionCompatibilityTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testOldToOld() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in1", "test", null);

        MuleMessage reply = client.request("vm://out1", TIMEOUT);
        assertNotNull(reply);
        assertEquals("test", reply.getPayload());
    }

    @Test
    public void testOldToNew() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in2", "test", null);

        MuleMessage reply = client.request("vm://out2", TIMEOUT);
        assertNotNull(reply);
        assertEquals("test", reply.getPayload());
    }

    @Test
    public void testNewToOld() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in3", "test", null);

        MuleMessage reply = client.request("vm://out3", TIMEOUT);
        // No output is received because the receiver throws an exception:
        // "java.lang.IllegalArgumentException: Session variable ... is malfomed and cannot be read"
        assertNull(reply);
    }

    @Test
    public void testNewToNew() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in4", "test", null);

        MuleMessage reply = client.request("vm://out4", TIMEOUT);
        assertNotNull(reply);
        assertEquals("test", reply.getPayload());
    }
}
