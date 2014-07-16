/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class RoundRobinTestCase extends FunctionalTestCase
{
    private static final int NUMBER_OF_MESSAGES = 10;
    private static final int NUMBER_OF_WRITERS = 10;
    private static final int NUMBER_OF_ENDPOINTS = 5;

    private MuleClient client;

    @Override
    protected String getConfigFile()
    {
        return "round-robin-test.xml";
    }

    @Test
    public void testRoundRobin() throws Exception
    {
        client = muleContext.getClient();
        List<Thread> writers = new ArrayList<Thread>();
        for (int i = 0; i < NUMBER_OF_WRITERS; i++)
        {
            writers.add(new Thread(new MessageWriter(i)));
        }
        for (Thread writer : writers)
        {
            writer.start();
        }
        for (Thread writer : writers)
        {
            writer.join();
        }

        for (int i = 0, j = 0; i < NUMBER_OF_WRITERS * NUMBER_OF_MESSAGES; i++)
        {
            // Message should be disrtibuted uniformly among endpoints
            String path = "vm://output" + j;
            MuleMessage msg = client.request(path, 0);
            assertNotNull(msg);
            logger.debug(path + ": " + msg.getPayloadAsString());
            j = (j + 1) % NUMBER_OF_ENDPOINTS;
        }
    }

    class MessageWriter implements Runnable
    {
        private int id;

        MessageWriter(int id)
        {
            this.id = id;
        }

        @Override
        public void run()
        {
            for (int i = 0; i < NUMBER_OF_MESSAGES; i++)
            {
                try
                {
                    client.send("vm://input", "Writer " + id + " Message " + i, null);
                }
                catch (MuleException ex)
                {
                    logger.info("Unexpected exception dispatching message", ex);
                }
            }
        }
    }
}