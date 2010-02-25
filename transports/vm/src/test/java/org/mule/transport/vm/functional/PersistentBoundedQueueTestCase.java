/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.vm.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class PersistentBoundedQueueTestCase extends FunctionalTestCase
{
    private static final int RECEIVE_TIMEOUT = 5000;
    // add some sizeable delat, as queue store ordering won't be guaranteed
    private static final int SLEEP = 100;

    protected String getConfigResources()
    {
        return "vm/persistent-bounded-vm-queue-test.xml";
    }

    public void testBoundedQueue() throws Exception
    {
        MuleClient client = new MuleClient();
        client.send("vm://in", "Test1", null);
        Thread.sleep(SLEEP);
        client.send("vm://in", "Test2", null);
        Thread.sleep(SLEEP);
        client.send("vm://in", "Test3", null);
        Thread.sleep(SLEEP);

        // wait enough for queue offer to timeout
        Thread.sleep(muleContext.getConfiguration().getDefaultQueueTimeout());
        
        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals("Test1", result.getPayloadAsString());
        result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals("Test2", result.getPayloadAsString());
        Thread.sleep(SLEEP);
        result = client.request("vm://out", RECEIVE_TIMEOUT);
        if (result != null) {
            System.out.println("result = " + result.getPayloadAsString());
        }
        assertNull(result);
    }

}
