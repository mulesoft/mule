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

import org.mule.MuleManager;
import org.mule.extras.client.MuleClient;
import org.mule.impl.internal.notifications.MessageNotification;
import org.mule.impl.internal.notifications.MessageNotificationListener;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestNotification;
import org.mule.tck.functional.FunctionalTestNotificationListener;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.util.concurrent.Latch;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.SerializationUtils;

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

    public void testMessageChunkingObject() throws Exception
    {
        final AtomicInteger messagePartsCount = new AtomicInteger(0);
        final Latch chunkingReceiverLatch = new Latch();
        final SimpleSerializableObject simpleSerializableObject = new SimpleSerializableObject("Test String", true, 99);

        // find number of chunks
        final int parts = (int)Math
            .ceil((SerializationUtils.serialize(simpleSerializableObject).length / (double)2));

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
                Object reply = ((FunctionalTestNotification)notification).getEventContext()
                    .getMessage().getPayload();
                // Check if Object is of Correct Type
                assertTrue(reply instanceof SimpleSerializableObject);
                SimpleSerializableObject replySimpleSerializableObject = (SimpleSerializableObject)reply;
                // Check that Contents are Identical
                assertEquals(simpleSerializableObject.b, replySimpleSerializableObject.b);
                assertEquals(simpleSerializableObject.i, replySimpleSerializableObject.i);
                assertEquals(simpleSerializableObject.s, replySimpleSerializableObject.s);
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
        client.dispatch("vm://inbound.channel", simpleSerializableObject, null);
        // Wait for the message to be received and tested (in the listener above)
        assertTrue(chunkingReceiverLatch.await(20L, TimeUnit.SECONDS));
        // Ensure we processed expected number of message parts
        assertEquals(parts, messagePartsCount.get());
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
