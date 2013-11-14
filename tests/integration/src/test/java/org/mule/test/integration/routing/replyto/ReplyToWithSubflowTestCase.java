/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.replyto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;


public class ReplyToWithSubflowTestCase extends FunctionalTestCase
{
    private static final String FLOW_PAYLOAD = "FLOW_PAYLOAD";
    private static final String SUB_FLOW_PAYLOAD = "SUB_FLOW_PAYLOAD";

    private static final String OUTPUT_QUEUE = "vm://outputQueue";

    private static final int DUPLICATE_RESPONSE_TIMEOUT = 500;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/replyto/replyto-with-subflow-test.xml";
    }

    @Test
    public void repliesOnlyOnceWhenUsingSyncSubflow() throws Exception
    {
        sendMessageAndExpectReply("syncSubFlow", "vm://syncInputQueue", SUB_FLOW_PAYLOAD);

    }

    @Test
    public void repliesOnlyOnceWhenUsingAsyncSubflow() throws Exception
    {
        sendMessageAndExpectReply("asyncSubFlow", "vm://asyncInputQueue", FLOW_PAYLOAD);
    }

    private void sendMessageAndExpectReply(String flow, String path, String expectedMessage) throws Exception
    {
        final CountDownLatch subFlowLatch = new CountDownLatch(1);

        getFunctionalTestComponent(flow).setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                subFlowLatch.countDown();
            }
        });

        // Send a test message to the endpoint with MULE_REPLY_TO property set.
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        message.setReplyTo(OUTPUT_QUEUE);
        muleContext.getClient().dispatch(path, message);

        // Expect to receive the reply
        message = muleContext.getClient().request(OUTPUT_QUEUE, RECEIVE_TIMEOUT);
        assertNotNull("Message was not received", message);
        assertEquals(expectedMessage, message.getPayload());

        // Wait for sub flow to process
        subFlowLatch.await(LOCK_TIMEOUT, TimeUnit.MILLISECONDS);

        // Assert that there are no more messages in the queue
        message = muleContext.getClient().request(OUTPUT_QUEUE, DUPLICATE_RESPONSE_TIMEOUT);
        assertNull("Response was received twice", message);
    }

}