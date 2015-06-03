/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor.chain;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.when;
import org.mule.MessageExchangePattern;
import org.mule.NonBlockingVoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorContainer;
import org.mule.api.processor.MessageProcessorPathElement;
import org.mule.api.processor.ProcessorExecutor;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.processor.NonBlockingProcessorExecutor;
import org.mule.tck.SensingNullReplyToHandler;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NonBlockingProcessorExecutorTestCase extends BlockingProcessorExecutorTestCase
{

    private static final int LATCH_TIMEOUT = 50;

    private SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();

    @Override
    public void before() throws MessagingException
    {
        super.before();
        when(event.getReplyToHandler()).thenReturn(nullReplyToHandler);
    }

    @Test
    @Override
    public void executeRequestResponseNonBlocking() throws MuleException, InterruptedException
    {
        setupNonBlockingMockEvent();
        assertNonBlockingExecution(processors);
    }

    @Test
    public void executeRequestResponseWithInterceptingMPBlocking() throws MuleException, InterruptedException
    {
        setupNonBlockingMockEvent();
        processors.clear();
        processors.add(new AbstractInterceptingMessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return processNext(event);
            }
        });
        processors.add(processor1);
        processors.add(processor2);
        processors.add(processor3);
        assertBlockingExecution(processors);
    }

    @Test
    public void executeRequestResponseWithMPContainerBlocking() throws MuleException, InterruptedException
    {
        setupNonBlockingMockEvent();
        processors.clear();
        processors.add(new TestContainerMessageProcessor());
        processors.add(processor2);
        processors.add(processor3);
        assertBlockingExecution(processors);
    }

    private void setupNonBlockingMockEvent()
    {
        when(event.getExchangePattern()).thenReturn(MessageExchangePattern.REQUEST_RESPONSE);
        when(event.isSynchronous()).thenReturn(false);
        when(event.isAllowNonBlocking()).thenReturn(true);
    }

    private class TestContainerMessageProcessor implements  MessageProcessor, MessageProcessorContainer{

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return processor1.process(event);
        }

        @Override
        public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement)
        {

        }
    }


    @Override
    protected ProcessorExecutor createProcessorExecutor(List<MessageProcessor> processors)
    {
        return new NonBlockingProcessorExecutor(event, processors, executionTemplate, true);
    }

    private void assertNonBlockingExecution(List<MessageProcessor> processors) throws MuleException, InterruptedException
    {
        ProcessorExecutor executor = createProcessorExecutor(processors);

        if (event.getExchangePattern() == MessageExchangePattern.REQUEST_RESPONSE)
        {
            assertThat(executor.execute(), CoreMatchers.<MuleEvent>equalTo(NonBlockingVoidMuleEvent.getInstance()));
        }
        else
        {
            assertThat(executor.execute(), equalTo(event));
        }

        nullReplyToHandler.latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS);

        assertThat(processor1.event, is(notNullValue()));
        assertThat(processor1.thread, equalTo(Thread.currentThread()));

        assertThat(processor2.event, is(notNullValue()));
        assertThat(processor2.thread, not(equalTo(processor1.thread)));

        assertThat(processor3.event, is(notNullValue()));
        assertThat(processor3.thread, not(equalTo(processor2.thread)));

        assertThat(nullReplyToHandler.event.getMessageAsString(), equalTo(RESULT));
    }

}