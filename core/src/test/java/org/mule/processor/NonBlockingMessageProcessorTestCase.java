/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.NonBlockingVoidMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.RequestContext;
import org.mule.VoidMuleEvent;
import org.mule.api.CompletionHandler;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.construct.Flow;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.SensingNullReplyToHandler;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.transformer.simple.StringAppendTransformer;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class NonBlockingMessageProcessorTestCase extends AbstractMuleContextTestCase
{

    private NonBlockingMessageProcessor nonBlockingMessageProcessor = new TestNonBlockingProcessor();

    @Test
    public void blockingProcess() throws MuleException, Exception
    {
        MuleEvent request = getTestEventUsingFlow(TEST_MESSAGE);
        MuleEvent response = nonBlockingMessageProcessor.process(request);

        // Test processor echos request so we can assert request equals response.
        assertThat(response, equalTo(request));
    }

    @Test
    public void nonBlockingProcessVoidMuleEventResponse() throws MuleException, Exception
    {
        MuleEvent request = createNonBlockingTestEvent();
        MuleEvent response = nonBlockingMessageProcessor.process(request);

        assertThat(response, is(instanceOf(NonBlockingVoidMuleEvent.class)));
    }

    @Test
    public void clearRequestContextAfterNonBlockingProcess() throws MuleException, Exception
    {
        MuleEvent request = createNonBlockingTestEvent();
        OptimizedRequestContext.unsafeSetEvent(request);
        MuleEvent response = nonBlockingMessageProcessor.process(request);

        assertThat(RequestContext.getEvent(), is(nullValue()));
    }

    private MuleEvent createNonBlockingTestEvent() throws Exception
    {
        Flow flow = MuleTestUtils.getTestFlow(muleContext);
        flow.setProcessingStrategy(new NonBlockingProcessingStrategy());
        return new DefaultMuleEvent(new DefaultMuleMessage(TEST_MESSAGE, muleContext),
                                                 MessageExchangePattern.REQUEST_RESPONSE,
                                                 new SensingNullReplyToHandler(), flow);
    }

    private class TestNonBlockingProcessor extends AbstractNonBlockingMessageProcessor
    {
        @Override
        protected MuleEvent processBlocking(MuleEvent event) throws MuleException
        {
            return event;
        }

        public void processNonBlocking(final MuleEvent event, CompletionHandler completionHandler)
        {
        }
    }
}


