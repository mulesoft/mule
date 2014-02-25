/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.devkit.processor;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.OptimizedRequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.security.oauth.processor.AbstractListeningMessageProcessor;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ListeningMessageProcessorWithListenerTest
{

    private static final String payload = "payload";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleContext muleContext;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FlowConstruct flowConstruct;

    protected AbstractListeningMessageProcessor processor;

    private MessageProcessor listener;

    @Before
    public void setUp() throws Exception
    {
        this.processor = new TestInterceptingMessageProcessor();
        this.processor.setMuleContext(this.muleContext);
        this.processor.setFlowConstruct(this.flowConstruct);

        this.listener = mock(MessageProcessor.class);
        when(this.listener.process(any(MuleEvent.class))).thenAnswer(new Answer<MuleEvent>()
        {
            @Override
            public MuleEvent answer(InvocationOnMock invocation) throws Throwable
            {
                MuleEvent event = (MuleEvent) invocation.getArguments()[0];
                MuleEvent response = getMuleEvent();
                when(response.getMessage().getPayload()).thenReturn(event.getMessage().getPayload());

                return response;
            }
        });
    }

    protected MuleEvent getMuleEvent()
    {
        MuleEvent event = mock(MuleEvent.class, RETURNS_DEEP_STUBS);
        when(event.getMessage().getPayload()).thenReturn(event);

        return event;
    }

    @Test
    public void withNoArgs() throws Exception
    {
        final String payload = "payload";
        MuleEvent event = this.getMuleEvent();
        when(event.getMessage().getPayload()).thenReturn(payload);

        OptimizedRequestContext.criticalSetEvent(event);

        assertEquals(payload, this.processor.process());
    }

    @Test
    public void withPayload() throws Exception
    {
        final String payload = "payload";
        this.processor.process(payload);
        assertEquals(payload, this.processor.process());
    }

    @Test
    public void withPayloadAndProperties() throws Exception
    {
        final String payload = "payload";

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("foo", "bar");

        this.processor.process(payload, props);
        assertEquals(payload, this.processor.process());
    }

    @Test
    public void processEvent() throws Exception
    {
        MuleEvent event = this.getMuleEvent();
        assertEquals(event.getMessage().getPayload(), this.processor.processEvent(event).getMessage().getPayload());
    }

    protected class TestInterceptingMessageProcessor extends AbstractListeningMessageProcessor
    {

        public TestInterceptingMessageProcessor()
        {
            super("");
        }

    }
}
