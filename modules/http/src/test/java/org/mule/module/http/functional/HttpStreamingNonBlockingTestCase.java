/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional;

import static org.junit.Assert.fail;
import static org.mule.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.api.config.MuleProperties.MULE_DEFAULT_PROCESSING_STRATEGY;
import static org.mule.config.spring.util.ProcessingStrategyUtils.NON_BLOCKING_PROCESSING_STRATEGY;
import org.mule.DefaultMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.transport.NonBlockingReplyToHandler;
import org.mule.api.transport.ReplyToHandler;
import org.mule.construct.Flow;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.concurrent.Latch;

import org.junit.Rule;

public class HttpStreamingNonBlockingTestCase extends HttpStreamingTestCase
{
    @Rule
    public SystemProperty nonBlocking = new SystemProperty(MULE_DEFAULT_PROCESSING_STRATEGY, NON_BLOCKING_PROCESSING_STRATEGY);

    @Override
    public void requesterStreams() throws Exception
    {
        FlowConstruct flow = muleContext.getRegistry().lookupFlowConstruct("client");
        Latch latch = new Latch();
        ReplyToHandler handler = new StopReplyToHandler(latch);
        MuleEvent event = new DefaultMuleEvent(getTestMuleMessage(), REQUEST_RESPONSE, handler, flow);
        ((Flow) flow).process(event);
        latch.await();
    }

    private static class StopReplyToHandler implements NonBlockingReplyToHandler
    {
        private final Latch latch;

        public StopReplyToHandler(Latch latch)
        {
            this.latch = latch;
        }

        @Override
        public void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException
        {
            stop.set(true);
            //consume stream
            event.getMessageAsString();
            latch.release();
        }

        @Override
        public void processExceptionReplyTo(MessagingException exception, Object replyTo)
        {
            fail();
        }
    }
}
