/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp.protocols;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class SafeProtocolTestCase extends AbstractServiceAndFlowTestCase
{
    protected static String TEST_MESSAGE = "Test TCP Request";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    public SafeProtocolTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "safe-protocol-test-service.xml"},
            {ConfigVariant.FLOW, "safe-protocol-test-flow.xml"}});
    }

    @Test
    public void testSafeToSafe() throws MuleException
    {
        MuleClient client = muleContext.getClient();
        assertResponseOk(client.send("tcp://localhost:" + dynamicPort1.getNumber() + "?connector=safe",
            TEST_MESSAGE, null));
    }

    @Test
    public void testUnsafeToSafe() throws MuleException
    {
        MuleClient client = muleContext.getClient();
        assertResponseBad(client.send("tcp://localhost:" + dynamicPort1.getNumber() + "?connector=unsafe",
            TEST_MESSAGE, null));
    }

    private void assertResponseOk(MuleMessage message)
    {
        assertNotNull("Null message", message);
        Object payload = message.getPayload();
        assertNotNull("Null payload", payload);
        assertTrue("Payload not byte[]", payload instanceof byte[]);
        assertEquals(TEST_MESSAGE + " Received", new String((byte[]) payload));
    }

    protected void assertResponseBad(MuleMessage message)
    {
        try
        {
            if (message.getPayloadAsString().equals(TEST_MESSAGE + " Received"))
            {
                fail("expected error");
            }
        }
        catch (Exception e)
        {
            // expected
        }
    }
}
