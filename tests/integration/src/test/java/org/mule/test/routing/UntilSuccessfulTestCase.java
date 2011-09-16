/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestComponent;

public class UntilSuccessfulTestCase extends FunctionalTestCase
{
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
        targetMessageProcessor = getFunctionalTestComponent("target-mp");
        deadLetterQueueProcessor = getFunctionalTestComponent("dlq-processor");
    }

    public void testDefaultConfiguration() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://input-1", "XYZ", null);
        ponderUntilMessageCountReceivedByTargetMessageProcessor(1);
    }

    public void testFullConfiguration() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        final MuleMessage response = client.send("vm://input-2", "XYZ", null);
        assertEquals("ACK", response.getPayloadAsString());
        ponderUntilMessageCountReceivedByTargetMessageProcessor(2);
        ponderUntilMessageCountReceivedByDlqProcessor(1);
    }

    public void testFullConfigurationMP() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        final MuleMessage response = client.send("vm://input-2MP", "XYZ", null);
        assertEquals("ACK", response.getPayloadAsString());
        ponderUntilMessageCountReceivedByTargetMessageProcessor(2);
        ponderUntilMessageCountReceivedByCustomMP(1);
    }

    public void testRetryOnEndpoint() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://input-3", "XYZ", null);
        ponderUntilMessageCountReceivedByTargetMessageProcessor(2);
    }

    private void ponderUntilMessageCountReceivedByTargetMessageProcessor(final int expectedCount)
        throws InterruptedException
    {
        ponderUntilMessageCountReceived(expectedCount, targetMessageProcessor);
    }

    private void ponderUntilMessageCountReceivedByDlqProcessor(final int expectedCount)
        throws InterruptedException
    {
        ponderUntilMessageCountReceived(expectedCount, deadLetterQueueProcessor);
    }

    private void ponderUntilMessageCountReceived(final int expectedCount, final FunctionalTestComponent ftc)
        throws InterruptedException
    {
        while (ftc.getReceivedMessagesCount() < expectedCount)
        {
            Thread.yield();
            Thread.sleep(100L);
        }
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
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            count++;
            return null;
        }
    }
}
