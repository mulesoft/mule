/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.vm.functional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class PersistentBoundedQueueTestCase extends FunctionalTestCase
{

    // add some sizeable delay, as queue store ordering won't be guaranteed
    private static final int SLEEP = 100;

    @Override
    protected String getConfigFile()
    {
        return "vm/persistent-bounded-vm-queue-test-flow.xml";
    }

    @Test
    public void testBoundedQueue() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in", "Test1", null);
        Thread.sleep(SLEEP);
        client.dispatch("vm://in", "Test2", null);
        Thread.sleep(SLEEP);
        client.dispatch("vm://in", "Test3", null);
        Thread.sleep(SLEEP);

        // wait enough for queue offer to timeout
        Thread.sleep(muleContext.getConfiguration().getDefaultQueueTimeout());

        // poll the 'out' queue 3 times, the first 2 times we must have a result. The
        // 3rd message must have been discarded as the queue was bounded.
        Set<String> results = new HashSet<String>();
        pollOutQueue(client, results);
        pollOutQueue(client, results);
        assertTrue(results.contains("Test1"));
        assertTrue(results.contains("Test2"));

        Thread.sleep(SLEEP);
        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        if (result != null)
        {
            System.out.println("result = " + getPayloadAsString(result));
        }
        assertNull(result);
    }

    private void pollOutQueue(MuleClient client, Set<String> results) throws Exception
    {
        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        results.add(getPayloadAsString(result));
    }
}
