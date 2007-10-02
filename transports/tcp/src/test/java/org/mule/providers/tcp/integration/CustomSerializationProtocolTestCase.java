/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.integration;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

/**
 * This test was set for the new changes due to Mule1199
 */
public class CustomSerializationProtocolTestCase extends FunctionalTestCase
{
    final private int messages = 1;

    public void testCustomObject() throws Exception
    {
        MuleClient client = new MuleClient();
        NonSerializableMessageObject message = new NonSerializableMessageObject(1, "Hello", true);

        for (int i = 0; i < messages; i++)
        {
            client.dispatch("vm://in", new MuleMessage(message));
        }

        for (int i = 0; i < messages; i++)
        {
            UMOMessage msg = client.receive("vm://out", 30000);
            assertTrue(msg.getPayload() instanceof NonSerializableMessageObject);
            NonSerializableMessageObject received = (NonSerializableMessageObject)msg.getPayload();
            assertEquals("Hello", received.s);
            assertEquals(1, received.i);
            assertEquals(true, received.b);
        }
    }

    protected String getConfigResources()
    {
        return "custom-serialisation-mule-config.xml";
    }

}
