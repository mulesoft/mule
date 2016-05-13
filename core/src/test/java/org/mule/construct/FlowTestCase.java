/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.MessageExchangePattern.REQUEST_RESPONSE;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.Message;
import org.mule.processor.ResponseMessageProcessorAdapter;
import org.mule.processor.chain.DynamicMessageProcessorContainer;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.transformer.simple.StringAppendTransformer;
import org.mule.util.NotificationUtils.FlowMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class FlowTestCase extends AbstractFlowConstuctTestCase
{

    private static final String FLOW_NAME = "test-flow";

    private Flow flow;
    private DynamicMessageProcessorContainer dynamicProcessorContainer;
    private SensingNullMessageProcessor sensingMessageProcessor;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        sensingMessageProcessor = getSensingNullMessageProcessor();

        flow = new Flow(FLOW_NAME, muleContext);
        flow.setMessageSource(directInboundMessageSource);

        dynamicProcessorContainer = mock(DynamicMessageProcessorContainer.class);
        when(dynamicProcessorContainer.process(any(MuleEvent.class))).then(new Answer<MuleEvent>()
        {
            @Override
            public MuleEvent answer(InvocationOnMock invocation) throws Throwable
            {
                Object[] args = invocation.getArguments();
                return (MuleEvent) args[0];
            }
        });

        List<MessageProcessor> processors = new ArrayList<MessageProcessor>();
        processors.add(new ResponseMessageProcessorAdapter(new StringAppendTransformer("f")));
        processors.add(new ResponseMessageProcessorAdapter(new StringAppendTransformer("e")));
        processors.add(new ResponseMessageProcessorAdapter(new StringAppendTransformer("d")));
        processors.add(new StringAppendTransformer("a"));
        processors.add(new StringAppendTransformer("b"));
        processors.add(new StringAppendTransformer("c"));
        processors.add(dynamicProcessorContainer);
        processors.add(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setOutboundProperty("thread", Thread.currentThread());
                return event;
            }
        });
        processors.add(sensingMessageProcessor);
        flow.setMessageProcessors(processors);
    }

    @Override
    protected AbstractFlowConstruct getFlowConstruct() throws Exception
    {
        return flow;
    }

    @Test
    public void testProcessOneWayEndpoint() throws Exception
    {
        flow.initialise();
        flow.start();
        MuleEvent response = directInboundMessageSource.process(MuleTestUtils.getTestEvent("hello",
            MessageExchangePattern.ONE_WAY, muleContext));
        Thread.sleep(50);

        assertNull(response);

        assertEquals("helloabc", sensingMessageProcessor.event.getMessageAsString());
        assertNotSame(Thread.currentThread(), sensingMessageProcessor.event.getMessage().getOutboundProperty(
            "thread"));
    }

    @Test
    public void testProcessRequestResponseEndpoint() throws Exception
    {
        flow.initialise();
        flow.start();
        MuleEvent response = directInboundMessageSource.process(MuleTestUtils.getTestEvent("hello",
            REQUEST_RESPONSE, muleContext));

        assertEquals("helloabcdef", response.getMessageAsString());
        assertEquals(Thread.currentThread(), response.getMessage().getOutboundProperty("thread"));

        // Sensed (out) event also is appended with 'def' because it's the same event
        // instance
        assertEquals("helloabcdef", sensingMessageProcessor.event.getMessageAsString());
        assertEquals(Thread.currentThread(), sensingMessageProcessor.event.getMessage().getOutboundProperty(
            "thread"));

    }

    @Test
    public void processorPath() throws MuleException
    {
        flow.initialise();
        flow.start();

        MessageProcessor processorInSubflow = new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return event;
            }
        };

        assertThat(flow.getProcessorPath(sensingMessageProcessor), is("/test-flow/processors/8"));
        assertThat(flow.getProcessorPath(dynamicProcessorContainer), is("/test-flow/processors/6"));
        assertThat(flow.getProcessorPath(processorInSubflow), is(nullValue()));

        reset(dynamicProcessorContainer);
        FlowMap dynamicContainerFlowMap = mock(FlowMap.class);
        when(dynamicContainerFlowMap.resolvePath(processorInSubflow)).thenReturn("/sub_dyn/subprocessors/0");
        when(dynamicContainerFlowMap.getFlowMap()).thenReturn(Collections.singletonMap(processorInSubflow, "/sub_dyn/subprocessors/0"));
        when(dynamicProcessorContainer.buildInnerPaths()).thenReturn(dynamicContainerFlowMap);
        assertThat(flow.getProcessorPath(processorInSubflow), is("/sub_dyn/subprocessors/0"));
        verify(dynamicProcessorContainer, times(1)).buildInnerPaths();

        flow.getProcessorPath(processorInSubflow);
        // No new invocation, the dynamic container reference was initialized and removed from the pending dynamic containers list.
        verify(dynamicProcessorContainer, times(1)).buildInnerPaths();
    }

    @Test
    public void testProcessStopped() throws Exception
    {
        flow.initialise();

        try
        {
            directInboundMessageSource.process(MuleTestUtils.getTestEvent("hello", muleContext));
            fail("exception expected");
        }
        catch (Exception e)
        {
        }
    }

    @Test
    public void testSequentialStageNames() throws Exception
    {
        final int count = 10;

        for (int i = 1; i <= count; i++)
        {
            assertTrue(this.flow.getAsyncStageNameSource().getName().endsWith("." + i));
        }
    }

    @Test
    public void testStageNameSourceWithName() throws Exception
    {
        final int count = 10;
        final String stageName = "myStage";
        final String EXPECTED = String.format("%s.%s", FLOW_NAME, stageName);

        for (int i = 0; i < count; i++)
        {
            assertEquals(EXPECTED, this.flow.getAsyncStageNameSource(stageName).getName());
        }
    }

    @Test
    public void testDynamicPipeline() throws Exception
    {
        flow.initialise();
        flow.start();

        MessageProcessor appendPre = new StringAppendTransformer("1");
        MessageProcessor appendPost2 = new StringAppendTransformer("4");

        String pipelineId = flow.dynamicPipeline(null).injectBefore(appendPre, new StringAppendTransformer("2"))
                .injectAfter(new StringAppendTransformer("3"), appendPost2)
                .resetAndUpdate();
        MuleEvent response = directInboundMessageSource.process(MuleTestUtils.getTestEvent("hello", REQUEST_RESPONSE, muleContext));
        assertEquals("hello12abcdef34", response.getMessageAsString());

        flow.dynamicPipeline(pipelineId).injectBefore(new StringAppendTransformer("2")).injectAfter(new StringAppendTransformer("3")).resetAndUpdate();
        response = directInboundMessageSource.process(MuleTestUtils.getTestEvent("hello", REQUEST_RESPONSE, muleContext));
        assertEquals("hello2abcdef3", response.getMessageAsString());

        flow.dynamicPipeline(pipelineId).reset();
        response = directInboundMessageSource.process(MuleTestUtils.getTestEvent("hello", REQUEST_RESPONSE, muleContext));
        assertEquals("helloabcdef", response.getMessageAsString());
    }

    @Test
    public void testFailStartingMessageSourceOnLifecycleShouldStopStartedPipelineProcesses() throws Exception
    {
        MessageSource mockMessageSource = mock(MessageSource.class, withSettings().extraInterfaces(Startable.class, Stoppable.class));
        doThrow(new LifecycleException(mock(Message.class), "Error starting component")).when(((Startable) mockMessageSource)).start();
        flow.setMessageSource(mockMessageSource);

        MessageProcessor mockMessageProcessor = mock(MessageProcessor.class, withSettings().extraInterfaces(Startable.class, Stoppable.class));
        flow.getMessageProcessors().add(mockMessageProcessor);

        flow.initialise();
        try
        {
            flow.start();
            fail();
        } catch (LifecycleException e)
        {
        }

        verify((Startable) mockMessageProcessor, times(1)).start();
        verify((Stoppable) mockMessageProcessor, times(1)).stop();

        verify((Startable) mockMessageSource, times(1)).start();
        verify((Stoppable) mockMessageSource, times(1)).stop();
    }
}
