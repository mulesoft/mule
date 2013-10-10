/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp.issues;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LengthProtocolLengthTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Override
    protected String getConfigResources()
    {
        return "length-protocol-length-test.xml";
    }

    @Test
    public void testLength() throws Exception
    {
        doTest("length", 5, true);
        doTest("length", 15, false);
    }

    @Test
    public void testSafe() throws Exception
    {
        doTest("safe", 5, true);
        doTest("safe", 15, false);
    }

    protected void doTest(String endpoint, int length, boolean ok) throws Exception
    {
        byte[] message = new byte[length];
        for (int i = 0; i < length; ++i)
        {
            message[i] = (byte)(i % 255);
        }
        MuleClient client = new MuleClient(muleContext);
        if (ok)
        {
            MuleMessage response = client.send(endpoint, message, null);
            assertNotNull(response);
            assertNotNull(response.getPayload());
            assertTrue(Arrays.equals(message, response.getPayloadAsBytes()));
        }
        else
        {
            assertResponseBad(client.send(endpoint, message, null));
        }
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
