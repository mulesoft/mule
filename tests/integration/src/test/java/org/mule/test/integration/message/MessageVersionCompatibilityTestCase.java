/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.message;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in1", "test", null);

        MuleMessage reply = client.request("vm://out1", TIMEOUT);
        assertNotNull(reply);
        assertEquals("test", reply.getPayload());
    }

    @Test
    public void testOldToNew() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in2", "test", null);

        MuleMessage reply = client.request("vm://out2", TIMEOUT);
        assertNotNull(reply);
        assertEquals("test", reply.getPayload());
    }

    @Test
    public void testNewToOld() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in3", "test", null);

        MuleMessage reply = client.request("vm://out3", TIMEOUT);
        // No output is received because the receiver throws an exception:
        // "java.lang.IllegalArgumentException: Session variable ... is malfomed and cannot be read"
        assertNull(reply);
    }

    @Test
    public void testNewToNew() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in4", "test", null);

        MuleMessage reply = client.request("vm://out4", TIMEOUT);
        assertNotNull(reply);
        assertEquals("test", reply.getPayload());
    }
}
