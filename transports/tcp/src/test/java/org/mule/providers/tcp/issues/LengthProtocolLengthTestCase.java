/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.issues;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import edu.emory.mathcs.backport.java.util.Arrays;

public class LengthProtocolLengthTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "length-protocol-length-test.xml";
    }

    public void testLength() throws Exception
    {
        doTest("length", 5, true);
        doTest("length", 15, false);
    }

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
        MuleClient client = new MuleClient();
        if (ok)
        {
            UMOMessage response = client.send(endpoint, message, null);
            assertNotNull(response);
            assertNotNull(response.getPayload());
            assertTrue(Arrays.equals(message, response.getPayloadAsBytes()));
        }
        else
        {
            assertResponseBad(client.send(endpoint, message, null));
        }
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
