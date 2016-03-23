/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.processor.MessageProcessor;
import org.mule.functional.junit4.FunctionalTestCase;

import org.junit.Test;

public class FlowAsyncBeforeAfterOutboundTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/construct/flow-async-before-after-outbound.xml";
    }

    @Test
    public void testAsyncBefore() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage msgSync = flowRunner("test-async-block-before-outbound").withPayload("message").run().getMessage();

        MuleMessage msgAsync = client.request("test://test.before.async.out", RECEIVE_TIMEOUT);
        MuleMessage msgOut = client.request("test://test.before.out", RECEIVE_TIMEOUT);

        assertCorrectThreads(msgSync, msgAsync, msgOut);

    }

    @Test
    public void testAsyncAfter() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage msgSync = flowRunner("test-async-block-after-outbound").withPayload("message").run().getMessage();

        MuleMessage msgAsync = client.request("test://test.after.async.out", RECEIVE_TIMEOUT);
        MuleMessage msgOut = client.request("test://test.after.out", RECEIVE_TIMEOUT);

        assertCorrectThreads(msgSync, msgAsync, msgOut);
    }

    private void assertCorrectThreads(MuleMessage msgSync, MuleMessage msgAsync, MuleMessage msgOut) throws Exception
    {
        assertNotNull(msgSync);
        assertNotNull(msgAsync);
        assertNotNull(msgOut);

        assertEquals((Object) msgSync.getInboundProperty("request-response-thread"),
            msgOut.getInboundProperty("request-response-thread"));

        assertTrue(!msgAsync.getOutboundProperty("async-thread").
            equals(msgSync.getOutboundProperty("request-response-thread")));

        assertTrue(!msgAsync.getOutboundProperty("async-thread").
            equals(msgOut.getOutboundProperty("request-response-thread")));
    }

    public static class ThreadSensingMessageProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            String propName = event.getFlowVariable("property-name");

            event.getMessage().setOutboundProperty(propName, Thread.currentThread().getName());
            return event;
        }
    }
}
