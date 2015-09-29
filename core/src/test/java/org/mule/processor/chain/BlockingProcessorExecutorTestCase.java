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
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.OptimizedRequestContext;
import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.ProcessorExecutor;
import org.mule.construct.Flow;
import org.mule.execution.MessageProcessorExecutionTemplate;
import org.mule.processor.BlockingProcessorExecutor;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class BlockingProcessorExecutorTestCase extends AbstractMuleTestCase
{

    @Mock(answer = RETURNS_DEEP_STUBS)
    protected MuleContext muleContext;

    @Mock
    protected MuleEvent event;

    @Mock
    protected MessageProcessorExecutionTemplate executionTemplate;

    protected static String A = "a";
    protected static String B = "b";
    protected static String C = "c";
    protected static String RESULT = A + B + C;

    protected SensingNullMessageProcessor processor1 = new SensingNullMessageProcessor(A);
    protected SensingNullMessageProcessor processor2 = new SensingNullMessageProcessor(B);
    protected SensingNullMessageProcessor processor3 = new SensingNullMessageProcessor(C);
    protected List<MessageProcessor> processors = new ArrayList<>();

    @Before
    public void before() throws MessagingException
    {
        processors.add(processor1);
        processors.add(processor2);
        processors.add(processor3);

        OptimizedRequestContext.unsafeSetEvent(event);
        
        when(event.getFlowConstruct()).thenReturn(mock(Flow.class));
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        when(event.getId()).thenReturn(RandomStringUtils.randomNumeric(3));
        when(event.getMessage()).thenReturn(message);
        when(event.getMuleContext()).thenReturn(muleContext);
        when(executionTemplate.execute(any(MessageProcessor.class), any(MuleEvent.class)))
                .thenAnswer(new Answer<MuleEvent>()
                {
                    @Override
                    public MuleEvent answer(InvocationOnMock invocation) throws Throwable
                    {
                        return ((MessageProcessor) invocation.getArguments()[0]).process((MuleEvent) invocation
                                .getArguments()[1]);
                    }
                });
    }

    @Test
    public void executeRequestResponse() throws MuleException
    {
        setupRequestResponseEvent();
        assertBlockingExecution(processors, requestResponseMatecher());
    }
    
    protected Matcher<MuleEvent> requestResponseMatecher() {
        return sameInstance(event);
    }

    @Test
    public void executeRequestResponseNullResponse() throws MuleException
    {
        processors.add(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return null;
            }
        });
        setupRequestResponseEvent();
        assertThat(createProcessorExecutor(processors).execute(), is(nullValue()));
    }

    @Test
    public void executeOneWay() throws MuleException
    {
        setupOneWayEvent();
        assertBlockingExecution(processors, sameInstance(event));
    }

    private void setupOneWayEvent()
    {
        when(event.getExchangePattern()).thenReturn(MessageExchangePattern.ONE_WAY);
        when(event.isAllowNonBlocking()).thenReturn(false);
        when(event.isSynchronous()).thenReturn(false);
    }

    protected void setupRequestResponseEvent()
    {
        when(event.getExchangePattern()).thenReturn(MessageExchangePattern.REQUEST_RESPONSE);
        when(event.isSynchronous()).thenReturn(true);
    }

    protected void assertBlockingExecution(List<MessageProcessor> processors, Matcher<MuleEvent> requestResponseMatcher) throws MuleException
    {
        ProcessorExecutor executor = createProcessorExecutor(processors);

        if (event.getExchangePattern() == MessageExchangePattern.REQUEST_RESPONSE)
        {
            assertThat(executor.execute().getMessageAsString(), equalTo(RESULT));
            assertThat(RequestContext.getEvent(), requestResponseMatcher);
        }
        else
        {
            assertThat(executor.execute().getId(), equalTo(event.getId()));
            assertThat(executor.execute().getMessage(), equalTo(event.getMessage()));
            assertThat(RequestContext.getEvent(), not(sameInstance(event)));
        }

        assertThat(processor1.event, is(notNullValue()));
        assertThat(processor1.thread, equalTo(Thread.currentThread()));

        assertThat(processor2.event, is(notNullValue()));
        assertThat(processor2.thread, equalTo(Thread.currentThread()));

        assertThat(processor3.event, is(notNullValue()));
        assertThat(processor3.thread, equalTo(Thread.currentThread()));
    }

    protected ProcessorExecutor createProcessorExecutor(List<MessageProcessor> processors)
    {
        return new BlockingProcessorExecutor(event, processors, executionTemplate, true);
    }
}