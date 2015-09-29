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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import org.mule.MessageExchangePattern;
import org.mule.NonBlockingVoidMuleEvent;
import org.mule.VoidMuleEvent;
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
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NonBlockingProcessorExecutorTestCase extends BlockingProcessorExecutorTestCase
{

    private static final int LATCH_TIMEOUT = 50;
    private static final String TEST_MESSAGE = "abc";

    private SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();

    @Override
    public void before() throws MessagingException
    {
        super.before();
        when(event.getReplyToHandler()).thenReturn(nullReplyToHandler);
        when(event.getExchangePattern()).thenReturn(MessageExchangePattern.REQUEST_RESPONSE);
        when(event.isSynchronous()).thenReturn(false);
        when(event.isAllowNonBlocking()).thenReturn(true);
    }
    
    protected Matcher<MuleEvent> requestResponseMatecher() {
        return not(sameInstance(event));
    }

    @Test
    public void executeRequestResponseNonBlocking() throws MuleException, InterruptedException
    {
        setupNonBlockingRequestResponseEvent();
        assertNonBlockingExecution(processors);
    }

    @Test
    public void executeRequestResponseWithInterceptingMPBlocking() throws MuleException, InterruptedException
    {
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
        assertBlockingExecution(processors, not(sameInstance(event)));
    }

    @Test
    public void executeRequestResponseWithMPContainerBlocking() throws MuleException, InterruptedException
    {
        processors.clear();
        processors.add(new TestContainerMessageProcessor());
        processors.add(processor2);
        processors.add(processor3);
        assertBlockingExecution(processors, not(sameInstance(event)));
    }

    @Test
    public void executeRequestResponseNonBlockingNullResponse() throws MuleException, InterruptedException
    {
        processors.add(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return null;
            }
        });
        setupNonBlockingRequestResponseEvent();
        createProcessorExecutor(processors).execute();
        assertThat(nullReplyToHandler.latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS), is(true));
        assertThat(nullReplyToHandler.event, is(nullValue()));
    }

    @Test
    public void executeRequestResponseNonBlockingVoidResponse() throws MuleException, InterruptedException
    {
        final MuleEvent voidResult = VoidMuleEvent.getInstance();
        processors.add(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return voidResult;
            }
        });
        setupNonBlockingRequestResponseEvent();
        MuleEvent request = event;
        createProcessorExecutor(processors).execute();
        assertThat(nullReplyToHandler.latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS), is(true));
        assertThat(nullReplyToHandler.event, is(not(nullValue())));
        assertThat(nullReplyToHandler.event, CoreMatchers.<MuleEvent>not(VoidMuleEvent.getInstance()));
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

        assertThat(nullReplyToHandler.latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS), is(true));

        assertThat(processor1.event, is(notNullValue()));
        assertThat(processor1.thread, equalTo(Thread.currentThread()));

        assertThat(processor2.event, is(notNullValue()));
        assertThat(processor2.thread, not(equalTo(processor1.thread)));

        assertThat(processor3.event, is(notNullValue()));
        assertThat(processor3.thread, not(equalTo(processor2.thread)));

        assertThat(nullReplyToHandler.event.getMessageAsString(), equalTo(RESULT));
    }


    private void setupNonBlockingRequestResponseEvent()
    {
        when(event.getExchangePattern()).thenReturn(MessageExchangePattern.REQUEST_RESPONSE);
        when(event.isSynchronous()).thenReturn(false);
        when(event.isAllowNonBlocking()).thenReturn(true);
        when(event.getReplyToHandler()).thenReturn(nullReplyToHandler);
    }

}