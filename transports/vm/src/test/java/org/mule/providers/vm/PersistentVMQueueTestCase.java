/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.vm;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class PersistentVMQueueTestCase extends FunctionalTestCase
{
    private static final int RECEIVE_TIMEOUT = 5000;

    protected String getConfigResources()
    {
        return "vm/persistent-vmqueue-test.xml";
    }

    public void testAsynchronousDispatching() throws Exception
    {
        String input = "Test message";
        String[] output = {"Test", "message"};
        MuleClient client = new MuleClient();
        client.dispatch("vm://receiver", input, null);
        UMOMessage result = client.receive("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertNotNull(result.getPayload());
        assertNull(result.getExceptionPayload());
        String[] payload = (String[]) result.getPayload();
        assertEquals(output.length, payload.length);
        for (int i = 0; i < output.length; i++)
        {
            assertEquals(output[i], payload[i]);
        }
    }

}
