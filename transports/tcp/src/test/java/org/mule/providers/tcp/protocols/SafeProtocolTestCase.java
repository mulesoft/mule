/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.protocols;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

public class SafeProtocolTestCase extends FunctionalTestCase
{

    protected static String TEST_MESSAGE = "Test TCP Request";

    protected String getConfigResources()
    {
        return "safe-protocol-test.xml";
    }

    public void testSafeToSafe() throws UMOException
    {
        MuleClient client = new MuleClient();
        assertResponseOk(client.send("tcp://localhost:65432?connector=safe", TEST_MESSAGE, null));
    }

    public void testUnsafeToSafe() throws UMOException
    {
        MuleClient client = new MuleClient();
        assertResponseBad(client.send("tcp://localhost:65432?connector=unsafe", TEST_MESSAGE, null));
    }

    private void assertResponseOk(UMOMessage message)
    {
        assertNotNull("Null message", message);
        Object payload = message.getPayload();
        assertNotNull("Null payload", payload);
        assertTrue("Payload not byte[]", payload instanceof byte[]);
        assertEquals(TEST_MESSAGE + " Received", new String((byte[]) payload));
    }

    protected void assertResponseBad(UMOMessage message)
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
