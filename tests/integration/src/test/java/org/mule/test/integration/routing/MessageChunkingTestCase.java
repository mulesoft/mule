/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import org.mule.MuleManager;
import org.mule.extras.client.MuleClient;
import org.mule.impl.internal.notifications.MessageNotification;
import org.mule.impl.internal.notifications.MessageNotificationListener;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestNotification;
import org.mule.tck.functional.FunctionalTestNotificationListener;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.util.concurrent.Latch;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MessageChunkingTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/message-chunking.xml";
    }

    public void testMessageChunkingWithEvenSplit() throws Exception
    {
        doMessageChunking("0123456789", 5);
    }

    public void testMessageChunkingWithOddSplit() throws Exception
    {
        doMessageChunking("01234567890", 6);
    }

    public void testMessageChunkingWith100Splits() throws Exception
    {
        doMessageChunking(
            "0123456789012345678901234567890123456789012345678901234567890123456789"
                            + "01234567890123456789012345678901234567890123456789012345678901234567890123456789"
                            + "01234567890123456789012345678901234567890123456789", 100);
    }

    public void testMessageChunkingOneChunk() throws Exception
    {
        doMessageChunking("x", 1);
    }

    protected void doMessageChunking(final String data, int partsCount) throws Exception
    {
        final AtomicInteger messagePartsCount = new AtomicInteger(0);
        final Latch chunkingReceiverLatch = new Latch();

        // Listen to events fired by the ChunkingReceiver component
        MuleManager.getInstance().registerListener(new FunctionalTestNotificationListener()
        {
            public void onNotification(UMOServerNotification notification)
            {
                // Not strictly necessary to test for this as when we register the
                // listener we
                // supply the ComponentName as the subscription filter
                assertEquals("ChunkingReceiver", notification.getResourceIdentifier());
                // Test that we have received all chunks in the correct order
                Object reply = ((FunctionalTestNotification)notification).getReplyMessage();
                assertEquals(data + " Received", reply);
                chunkingReceiverLatch.countDown();
            }
        }, "ChunkingReceiver");

        // Listen to Message Notifications on the Chunking receiver so we can
        // determine how
        // many message parts have been received
        MuleManager.getInstance().registerListener(new MessageNotificationListener()
        {
            public void onNotification(UMOServerNotification notification)
            {
                if (notification.getAction() == MessageNotification.MESSAGE_RECEIVED)
                {
                    messagePartsCount.getAndIncrement();
                }
                assertEquals("ChunkingReceiver", notification.getResourceIdentifier());
            }
        }, "ChunkingReceiver");

        MuleClient client = new MuleClient();
        client.dispatch("vm://inbound.channel", data, null);
        // Wait for the message to be received and tested (in the listener above)
        assertTrue(chunkingReceiverLatch.await(20L, TimeUnit.SECONDS));
        // Ensure we processed expected number of message parts
        assertEquals(partsCount, messagePartsCount.get());
    }
}
