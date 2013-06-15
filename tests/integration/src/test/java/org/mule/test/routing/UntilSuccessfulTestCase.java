/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.store.AbstractPartitionedObjectStore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

public class UntilSuccessfulTestCase extends FunctionalTestCase
{
    private MuleClient client;
    private FunctionalTestComponent targetMessageProcessor;
    private FunctionalTestComponent deadLetterQueueProcessor;

    @Override
    protected String getConfigResources()
    {
        return "until-successful-test.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        client = muleContext.getClient();

        targetMessageProcessor = getFunctionalTestComponent("target-mp");
        deadLetterQueueProcessor = getFunctionalTestComponent("dlq-processor");

        final AbstractPartitionedObjectStore<Serializable> objectStore = muleContext.getRegistry()
            .lookupObject("objectStore");
        objectStore.disposePartition("DEFAULT_PARTITION");
    }

    @Test
    public void testDefaultConfiguration() throws Exception
    {
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        client.dispatch("vm://input-1", payload, null);

        final List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(1);
        assertEquals(1, receivedPayloads.size());
        assertEquals(payload, receivedPayloads.get(0));
    }

    @Test
    public void testFullConfiguration() throws Exception
    {
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        final MuleMessage response = client.send("vm://input-2", payload, null);
        assertEquals("ACK", response.getPayloadAsString());

        List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(3);
        assertEquals(3, receivedPayloads.size());
        for (int i = 0; i <= 2; i++)
        {
            assertEquals(payload, receivedPayloads.get(i));
        }

        receivedPayloads = ponderUntilMessageCountReceivedByDlqProcessor(1);
        assertEquals(1, receivedPayloads.size());
        assertEquals(payload, receivedPayloads.get(0));
    }

    @Test
    public void testFullConfigurationMP() throws Exception
    {
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        final MuleMessage response = client.send("vm://input-2MP", payload, null);
        assertEquals("ACK", response.getPayloadAsString());

        final List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(3);
        assertEquals(3, receivedPayloads.size());
        for (int i = 0; i <= 2; i++)
        {
            assertEquals(payload, receivedPayloads.get(i));
        }

        ponderUntilMessageCountReceivedByCustomMP(1);
    }

    @Test
    public void testRetryOnEndpoint() throws Exception
    {
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        client.dispatch("vm://input-3", payload, null);

        final List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(3);
        assertEquals(3, receivedPayloads.size());
        for (int i = 0; i <= 2; i++)
        {
            assertEquals(payload, receivedPayloads.get(i));
        }
    }

    private List<Object> ponderUntilMessageCountReceivedByTargetMessageProcessor(final int expectedCount)
        throws InterruptedException
    {
        return ponderUntilMessageCountReceived(expectedCount, targetMessageProcessor);
    }

    private List<Object> ponderUntilMessageCountReceivedByDlqProcessor(final int expectedCount)
        throws InterruptedException
    {
        return ponderUntilMessageCountReceived(expectedCount, deadLetterQueueProcessor);
    }

    private List<Object> ponderUntilMessageCountReceived(final int expectedCount,
                                                         final FunctionalTestComponent ftc)
        throws InterruptedException
    {
        final List<Object> results = new ArrayList<Object>();

        while (ftc.getReceivedMessagesCount() < expectedCount)
        {
            Thread.yield();
            Thread.sleep(100L);
        }

        for (int i = 0; i < ftc.getReceivedMessagesCount(); i++)
        {
            results.add(ftc.getReceivedMessage(1 + i));
        }
        return results;
    }

    private void ponderUntilMessageCountReceivedByCustomMP(final int expectedCount)
        throws InterruptedException
    {
        while (CustomMP.getCount() < expectedCount)
        {
            Thread.yield();
            Thread.sleep(100L);
        }
    }

    static class CustomMP implements MessageProcessor
    {
        private static int count;

        public static void clearCount()
        {
            count = 0;
        }

        public static int getCount()
        {
            return count;
        }

        @Override
        public MuleEvent process(final MuleEvent event) throws MuleException
        {
            count++;
            return null;
        }
    }
}
