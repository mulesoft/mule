/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.udp.functional;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashSet;
import java.util.Set;

public class UdpConnectorFunctionalTestCase extends FunctionalTestCase
{

    public static final String MESSAGE = "hello";
    public static final int TOTAL_MESSAGE_COUNT = 1000;
    public static final int MAX_NUMBER_OF_BATCHES = 128;
    public static final long MAX_PAUSE_PERIOD = 2000;
    public static final long MIN_PAUSE_PERIOD = 10;
    public static final long BETWEEN_BATCH_PAUSE = 5000;

    protected String getConfigResources()
    {
        return "udp-functional-test.xml";
    }

    /**
     * We try progressively smaller batches to see if there are issues with internal
     * buffers.  If we don't get 100% success eventually, we fail.
     */
    public void testMany() throws Exception
    {
        int numberOfBatches = 0;
        boolean ok = false;
        while (!ok && numberOfBatches < MAX_NUMBER_OF_BATCHES)
        {
            numberOfBatches = 0 == numberOfBatches ? 1 : numberOfBatches * 2;
            ok = doTestSome(TOTAL_MESSAGE_COUNT, TOTAL_MESSAGE_COUNT / numberOfBatches);
            if (!ok)
            {
                logger.warn("UDP failed to send " + TOTAL_MESSAGE_COUNT + " messages in " + numberOfBatches + " batches");

                // clean out the system
                try
                {
                    synchronized(this)
                    {
                        this.wait(BETWEEN_BATCH_PAUSE);
                    }
                }
                catch (InterruptedException e)
                {
                    // ignore
                }
                MuleClient client = new MuleClient();
                int dropped = 0;
                while (null != client.receive("vm://foo", MAX_PAUSE_PERIOD))
                {
                    // discard old messages
                    dropped++;
                }
                logger.info("Cleaned out " + dropped + " messages");
            }
        }

        if (!ok)
        {
            fail("Couldn't get UDP to 100% with a batch size of " + TOTAL_MESSAGE_COUNT / numberOfBatches);
        }
        else
        {
            logger.info("Required " + numberOfBatches + " batches before UDP 100% OK ("
                    + TOTAL_MESSAGE_COUNT + " messages)");
        }
    }

    /**
     * @param numberOfMessages Total number of tests
     * @param burst Number of mesages to send between wait periods
     * @return true if all messages received
     * @throws Exception
     */
    protected boolean doTestSome(int numberOfMessages, int burst) throws Exception
    {
        logger.info("Trying " + numberOfMessages + " messages in batches of " + burst);
        MuleClient client = new MuleClient();

        int burstCount = 0;
        Set receivedMessages = new HashSet(numberOfMessages);
        for (int sentPackets = 0; sentPackets < numberOfMessages; sentPackets++)
        {
            burstCount++;
            String msg = MESSAGE + sentPackets;
            client.dispatch("serverEndpoint", msg, null);

            if (burst == burstCount || sentPackets == numberOfMessages-1)
            {
                long pause = MAX_PAUSE_PERIOD;
                for (int i = 0; i < burstCount; i++)
                {
                    UMOMessage message = client.receive("vm://foo", pause);
                    // reduce waiting time once we have a bunch of messages coming in
                    // (without this, we can end up waiting for very long times....)
                    pause = Math.max(MIN_PAUSE_PERIOD, pause / 2);
                    if (null != message)
                    {
                        receivedMessages.add(message.getPayloadAsString());
                    }
                }
                burstCount = 0;
            }
        }

        boolean ok = receivedMessages.size() == numberOfMessages;
        if (!ok)
        {
            logger.info("Received " + receivedMessages.size() + " messages");
        }
        return ok;
    }

}
