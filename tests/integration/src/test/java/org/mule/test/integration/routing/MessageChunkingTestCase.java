/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.functional.functional.FunctionalTestNotification;
import org.mule.functional.functional.FunctionalTestNotificationListener;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.functional.listener.FlowExecutionListener;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

public class MessageChunkingTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/message-chunking-flow.xml";
    }

    @Test
    public void testMessageChunkingWithEvenSplit() throws Exception
    {
        doMessageChunking("0123456789", 5);
    }

    @Test
    public void testMessageChunkingWithOddSplit() throws Exception
    {
        doMessageChunking("01234567890", 6);
    }

    @Test
    public void testMessageChunkingWith100Splits() throws Exception
    {
        doMessageChunking(
            "0123456789012345678901234567890123456789012345678901234567890123456789"
                            + "01234567890123456789012345678901234567890123456789012345678901234567890123456789"
                            + "01234567890123456789012345678901234567890123456789", 100);
    }

    @Test
    public void testMessageChunkingOneChunk() throws Exception
    {
        doMessageChunking("x", 1);
    }

    @Test
    public void testMessageChunkingObject() throws Exception
    {
        final AtomicInteger messagePartsCount = new AtomicInteger(0);
        final Latch chunkingReceiverLatch = new Latch();
        final SimpleSerializableObject simpleSerializableObject = new SimpleSerializableObject("Test String",
            true, 99);

        // find number of chunks
        final int parts = (int) Math.ceil((SerializationUtils.serialize(simpleSerializableObject).length / (double) 2));

        // Listen to events fired by the ChunkingReceiver service
        muleContext.registerListener(new FunctionalTestNotificationListener()
        {
            @Override
            public void onNotification(ServerNotification notification)
            {
                // Not strictly necessary to test for this as when we register the
                // listener we supply the ComponentName as the subscription filter
                assertEquals("ChunkingObjectReceiver", notification.getResourceIdentifier());
                // Test that we have received all chunks in the correct order
                Object reply = ((FunctionalTestNotification) notification).getEventContext()
                    .getMessage()
                    .getPayload();
                // Check if Object is of Correct Type
                assertTrue(reply instanceof SimpleSerializableObject);
                SimpleSerializableObject replySimpleSerializableObject = (SimpleSerializableObject) reply;
                // Check that Contents are Identical
                assertEquals(simpleSerializableObject.b, replySimpleSerializableObject.b);
                assertEquals(simpleSerializableObject.i, replySimpleSerializableObject.i);
                assertEquals(simpleSerializableObject.s, replySimpleSerializableObject.s);
                chunkingReceiverLatch.countDown();
            }
        }, "ChunkingObjectReceiver");

        // Listen to Message Notifications on the Chunking receiver so we can
        // determine how many message parts have been received
        FlowExecutionListener flowExecutionListener = new FlowExecutionListener("ChunkingObjectReceiver", muleContext);
        flowExecutionListener.addListener(source -> messagePartsCount.getAndIncrement());

        MuleClient client = muleContext.getClient();
        flowRunner("ObjectReceiver").withPayload(simpleSerializableObject).asynchronously().run();
        // Wait for the message to be received and tested (in the listener above)
        assertTrue(chunkingReceiverLatch.await(20L, TimeUnit.SECONDS));
        // Ensure we processed expected number of message parts
        assertEquals(parts, messagePartsCount.get());
    }

    protected void doMessageChunking(final String data, int partsCount) throws Exception
    {
        final AtomicInteger messagePartsCount = new AtomicInteger(0);
        final Latch chunkingReceiverLatch = new Latch();

        // Listen to events fired by the ChunkingReceiver service
        muleContext.registerListener(new FunctionalTestNotificationListener()
        {
            @Override
            public void onNotification(ServerNotification notification)
            {
                // Not strictly necessary to test for this as when we register the
                // listener we supply the ComponentName as the subscription filter
                assertEquals("ChunkingReceiver", notification.getResourceIdentifier());

                // Test that we have received all chunks in the correct order
                Object reply = ((FunctionalTestNotification) notification).getReplyMessage();
                assertEquals(data + " Received", reply);
                chunkingReceiverLatch.countDown();
            }
        }, "ChunkingReceiver");

        // Listen to Message Notifications on the Chunking receiver so we can
        // determine how many message parts have been received
        FlowExecutionListener flowExecutionListener = new FlowExecutionListener("ChunkingReceiver", muleContext);
        flowExecutionListener.addListener(source -> messagePartsCount.getAndIncrement());
        flowRunner("Receiver").withPayload(data).asynchronously().run();
        // Wait for the message to be received and tested (in the listener above)
        assertTrue(chunkingReceiverLatch.await(20L, TimeUnit.SECONDS));
        // Ensure we processed expected number of message parts
        assertEquals(partsCount, messagePartsCount.get());
    }
}
