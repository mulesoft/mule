/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct.processor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.config.ChainedThreadingProfile;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.construct.AbstractPipeline;
import org.mule.construct.Flow;
import org.mule.context.notification.AsyncMessageNotification;
import org.mule.context.notification.ExceptionStrategyNotification;
import org.mule.context.notification.PipelineMessageNotification;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.endpoint.AbstractMessageProcessorTestCase;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.management.stats.AllStatistics;
import org.mule.processor.ResponseMessageProcessorAdapter;
import org.mule.processor.strategy.AsynchronousProcessingStrategy;
import org.mule.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.registry.DefaultRegistryBroker;
import org.mule.registry.MuleRegistryHelper;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.SensingNullReplyToHandler;
import org.mule.tck.TriggerableMessageSource;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.transformer.simple.StringAppendTransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PipelineMessageNotificationTestCase extends AbstractMuleTestCase
{

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleContext muleContext;
    private MuleEvent event;
    private ServerNotificationManager notificationManager;
    private TestPipeline pipeline;
    private final String pipelineName = "testPipeline";

    @Before
    public void createMocks() throws Exception
    {

        when(muleContext.getStatistics()).thenReturn(new AllStatistics());
        when(muleContext.getConfiguration()).thenReturn(new DefaultMuleConfiguration());
        when(muleContext.getRegistry()).thenReturn(
                new MuleRegistryHelper(new DefaultRegistryBroker(muleContext), muleContext));
        when(muleContext.getDefaultThreadingProfile()).thenReturn(new ChainedThreadingProfile());
        notificationManager = mock(ServerNotificationManager.class);
        when(muleContext.getNotificationManager()).thenReturn(notificationManager);
        pipeline = new TestPipeline(pipelineName, muleContext);

    }

    @Test
    public void requestResponse() throws MuleException
    {
        TriggerableMessageSource source = new TriggerableMessageSource();
        pipeline.setMessageSource(source);
        pipeline.initialise();

        event = new DefaultMuleEvent(new DefaultMuleMessage("request", muleContext),
                                     MessageExchangePattern.REQUEST_RESPONSE, pipeline);

        source.trigger(event);

        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        PipelineMessageNotification.PROCESS_START, false, event)));
        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(PipelineMessageNotification.PROCESS_END,
                                                                        false, event)));
        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        PipelineMessageNotification.PROCESS_COMPLETE, false, event)));
        verify(notificationManager, times(3)).fireNotification(any(PipelineMessageNotification.class));
    }

    @Test
    public void requestResponseNonBlocking() throws Exception
    {
        assertRequestResponseNonBlockingWithMessageProcessor(new SensingNullMessageProcessor());
    }

    @Test
    public void requestResponseNonBlockingWithBlockingMessageProcessor() throws Exception
    {
        assertRequestResponseNonBlockingWithMessageProcessor(new StringAppendTransformer(""));
    }

    private void assertRequestResponseNonBlockingWithMessageProcessor(MessageProcessor messageProcessor) throws Exception
    {
        TriggerableMessageSource source = new TriggerableMessageSource();
        pipeline.setMessageSource(source);
        pipeline.setProcessingStrategy(new NonBlockingProcessingStrategy());
        pipeline.setMessageProcessors(Collections.singletonList(messageProcessor));
        pipeline.initialise();

        SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();
        event = new DefaultMuleEvent(new DefaultMuleMessage("request", muleContext),
                                     MessageExchangePattern.REQUEST_RESPONSE, nullReplyToHandler, pipeline);

        source.trigger(event);
        nullReplyToHandler.latch.await(AbstractMuleContextTestCase.RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);

        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        PipelineMessageNotification.PROCESS_START, false, null)));
        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(PipelineMessageNotification.PROCESS_END,
                                                                        false, null)));
        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        PipelineMessageNotification.PROCESS_COMPLETE, false, null)));
        verify(notificationManager, times(3)).fireNotification(any(PipelineMessageNotification.class));
    }

    @Test
    public void oneWay() throws MuleException, InterruptedException
    {
        TriggerableMessageSource source = new TriggerableMessageSource();
        pipeline.setMessageSource(source);
        pipeline.initialise();

        event = new DefaultMuleEvent(new DefaultMuleMessage("request", muleContext),
                                     MessageExchangePattern.ONE_WAY, pipeline);

        source.trigger(event);
        pipeline.latch.await(AbstractMuleContextTestCase.RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);

        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        PipelineMessageNotification.PROCESS_START, false, event)));
        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(PipelineMessageNotification.PROCESS_END,
                                                                        false, event)));
        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        PipelineMessageNotification.PROCESS_COMPLETE, false, event)));
        verify(notificationManager, times(3)).fireNotification(any(PipelineMessageNotification.class));
    }

    @Test
    public void requestResponseRequestException() throws MuleException, InterruptedException
    {
        TriggerableMessageSource source = new TriggerableMessageSource();
        pipeline.setMessageSource(source);
        pipeline.setExceptionListener(new DefaultMessagingExceptionStrategy());
        List<MessageProcessor> processors = new ArrayList<MessageProcessor>();
        processors.add(new AbstractMessageProcessorTestCase.ExceptionThrowingMessageProcessr());
        pipeline.setMessageProcessors(processors);
        pipeline.initialise();

        event = new DefaultMuleEvent(new DefaultMuleMessage("request", muleContext),
                                     MessageExchangePattern.REQUEST_RESPONSE, pipeline);

        try
        {
            source.trigger(event);
        }
        catch (Exception e)
        {
        }

        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        PipelineMessageNotification.PROCESS_START, false, event)));
        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        PipelineMessageNotification.PROCESS_COMPLETE, true, event)));
        verify(notificationManager, times(2)).fireNotification(any(PipelineMessageNotification.class));
    }

    @Test
    public void requestResponseNonBlockingRequestException() throws MuleException, InterruptedException
    {
        TriggerableMessageSource source = new TriggerableMessageSource();
        pipeline.setMessageSource(source);
        pipeline.setExceptionListener(new DefaultMessagingExceptionStrategy());
        List<MessageProcessor> processors = new ArrayList<MessageProcessor>();
        processors.add(new AbstractMessageProcessorTestCase.ExceptionThrowingMessageProcessr());
        processors.add(new SensingNullMessageProcessor());
        pipeline.setMessageProcessors(processors);
        pipeline.setProcessingStrategy(new NonBlockingProcessingStrategy());
        pipeline.initialise();

        SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();
        event = new DefaultMuleEvent(new DefaultMuleMessage("request", muleContext),
                                     MessageExchangePattern.REQUEST_RESPONSE, nullReplyToHandler, pipeline);

        try
        {
            source.trigger(event);
        }
        catch (Exception e)
        {
        }

        nullReplyToHandler.latch.await(AbstractMuleContextTestCase.RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);

        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        PipelineMessageNotification.PROCESS_START, false, null)));
        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        PipelineMessageNotification.PROCESS_COMPLETE, true, null)));
        verify(notificationManager, times(2)).fireNotification(any(PipelineMessageNotification.class));
    }

    @Test
    public void requestResponseNonBlockingResponseException() throws MuleException, InterruptedException
    {
        TriggerableMessageSource source = new TriggerableMessageSource();
        pipeline.setMessageSource(source);
        pipeline.setExceptionListener(new DefaultMessagingExceptionStrategy());
        List<MessageProcessor> processors = new ArrayList<MessageProcessor>();
        processors.add(new ResponseMessageProcessorAdapter(new AbstractMessageProcessorTestCase
                .ExceptionThrowingMessageProcessr()));
        processors.add(new SensingNullMessageProcessor());
        pipeline.setMessageProcessors(processors);
        pipeline.setProcessingStrategy(new NonBlockingProcessingStrategy());
        pipeline.initialise();

        SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();
        event = new DefaultMuleEvent(new DefaultMuleMessage("request", muleContext),
                                     MessageExchangePattern.REQUEST_RESPONSE, nullReplyToHandler, pipeline);

        try
        {
            source.trigger(event);
        }
        catch (Exception e)
        {
        }

        nullReplyToHandler.latch.await(AbstractMuleContextTestCase.RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);

        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        PipelineMessageNotification.PROCESS_START, false, null)));
        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(PipelineMessageNotification.PROCESS_END,
                                                                        false, null)));
        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        PipelineMessageNotification.PROCESS_COMPLETE, true, null)));
        verify(notificationManager, times(3)).fireNotification(any(PipelineMessageNotification.class));
    }

    @Test
    public void requestResponseResponseException() throws MuleException, InterruptedException
    {
        TriggerableMessageSource source = new TriggerableMessageSource();
        pipeline.setMessageSource(source);
        pipeline.setExceptionListener(new DefaultMessagingExceptionStrategy());
        List<MessageProcessor> processors = new ArrayList<MessageProcessor>();
        processors.add(new ResponseMessageProcessorAdapter(new AbstractMessageProcessorTestCase
                .ExceptionThrowingMessageProcessr()));
        pipeline.setMessageProcessors(processors);
        pipeline.initialise();

        event = new DefaultMuleEvent(new DefaultMuleMessage("request", muleContext),
                                     MessageExchangePattern.REQUEST_RESPONSE, pipeline);

        try
        {
            source.trigger(event);
        }
        catch (Exception e)
        {
        }
        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        PipelineMessageNotification.PROCESS_START, false, event)));
        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(PipelineMessageNotification.PROCESS_END,
                                                                        false, event)));
        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        PipelineMessageNotification.PROCESS_COMPLETE, true, event)));
        verify(notificationManager, times(3)).fireNotification(any(PipelineMessageNotification.class));
    }

    @Test
    public void oneWayRequestException() throws MuleException, InterruptedException
    {
        TriggerableMessageSource source = new TriggerableMessageSource();
        pipeline.setMessageSource(source);
        pipeline.setExceptionListener(new DefaultMessagingExceptionStrategy());
        List<MessageProcessor> processors = new ArrayList<MessageProcessor>();
        processors.add(new AbstractMessageProcessorTestCase
                .ExceptionThrowingMessageProcessr());
        pipeline.setMessageProcessors(processors);
        pipeline.initialise();

        event = new DefaultMuleEvent(new DefaultMuleMessage("request", muleContext),
                                     MessageExchangePattern.ONE_WAY, pipeline);

        try
        {
            source.trigger(event);
        }
        catch (Exception e)
        {
        }
        verify(notificationManager, times(1)).fireNotification(
            argThat(new PipelineMessageNotificiationArgumentMatcher(
                PipelineMessageNotification.PROCESS_START, false, event)));
        verify(notificationManager, times(1)).fireNotification(
            argThat(new PipelineMessageNotificiationArgumentMatcher(PipelineMessageNotification.PROCESS_COMPLETE, true, null)));
        verify(notificationManager, times(2)).fireNotification(any(PipelineMessageNotification.class));
    }

    @Test
    public void oneWayAsyncRequestException() throws MuleException, InterruptedException
    {
        TriggerableMessageSource source = new TriggerableMessageSource();
        Flow pipeline = new Flow("test", muleContext);
        pipeline.setProcessingStrategy(new AsynchronousProcessingStrategy());
        final CountDownLatch latch = new CountDownLatch(1);
        pipeline.setMessageSource(source);
        pipeline.setExceptionListener(new DefaultMessagingExceptionStrategy());
        List<MessageProcessor> processors = new ArrayList<MessageProcessor>();
        processors.add(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                latch.countDown();
                throw new RuntimeException("error");
            }
        });
        pipeline.setMessageProcessors(processors);
        pipeline.initialise();
        pipeline.start();

        event = new DefaultMuleEvent(new DefaultMuleMessage("request", muleContext),
                                     MessageExchangePattern.ONE_WAY, pipeline);

        source.trigger(event);
        latch.await(AbstractMuleContextTestCase.RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);
        Thread.sleep(2000);

        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        PipelineMessageNotification.PROCESS_START, false, event)));
        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED, false, event)));
        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        PipelineMessageNotification.PROCESS_COMPLETE, false, null)));
        // Event is not same, because it's copied
        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        AsyncMessageNotification.PROCESS_ASYNC_COMPLETE, false, null)));
        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        ExceptionStrategyNotification.PROCESS_START, false, null)));
        verify(notificationManager, times(1)).fireNotification(
                argThat(new PipelineMessageNotificiationArgumentMatcher(
                        ExceptionStrategyNotification.PROCESS_END, false, null)));
        verify(notificationManager, times(6)).fireNotification(any(PipelineMessageNotification.class));
    }


    private class TestPipeline extends AbstractPipeline
    {

        CountDownLatch latch = new CountDownLatch(2);

        public TestPipeline(String name, MuleContext muleContext)
        {
            super(name, muleContext);
        }

        @Override
        protected void configureMessageProcessors(MessageProcessorChainBuilder builder) throws MuleException
        {
            builder.chain(new MessageProcessor()
            {
                @Override
                public MuleEvent process(MuleEvent event) throws MuleException
                {
                    latch.countDown();
                    return event;
                }
            });
            super.configureMessageProcessors(builder);
        }

        @Override
        protected void configurePostProcessors(MessageProcessorChainBuilder builder) throws MuleException
        {
            super.configurePostProcessors(builder);
            builder.chain(new MessageProcessor()
            {

                @Override
                public MuleEvent process(MuleEvent event) throws MuleException
                {
                    latch.countDown();
                    return event;
                }
            });
        }

        @Override
        public String getConstructType()
        {
            return "test";
        }
    }

    private class PipelineMessageNotificiationArgumentMatcher extends
        ArgumentMatcher<PipelineMessageNotification>
    {
        private int expectedAction;
        private boolean exceptionExpected;
        private MuleEvent event;

        public PipelineMessageNotificiationArgumentMatcher(int expectedAction,
                                                           boolean exceptionExpected,
                                                           MuleEvent event)
        {
            this.expectedAction = expectedAction;
            this.exceptionExpected = exceptionExpected;
            this.event = event;
        }

        @Override
        public boolean matches(Object argument)
        {
            ServerNotification notification = (ServerNotification) argument;
            if (exceptionExpected)
            {
                MessagingException exception = null;
                if (notification instanceof PipelineMessageNotification)
                {
                    exception = ((PipelineMessageNotification) notification).getException();
                }
                else if (notification instanceof AsyncMessageNotification)
                {
                    exception = ((AsyncMessageNotification) notification).getException();

                }
                return expectedAction == notification.getAction() && exception != null
                       && notification.getSource() != null
                       && (this.event == null || this.event == notification.getSource());
            }
            else
            {
                return expectedAction == notification.getAction() && notification.getSource() != null
                       && (this.event == null || this.event == notification.getSource());
            }

        }
    }

}
