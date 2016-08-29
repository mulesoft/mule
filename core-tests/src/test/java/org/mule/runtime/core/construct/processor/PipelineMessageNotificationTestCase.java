/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.construct.processor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.context.notification.AsyncMessageNotification.PROCESS_ASYNC_COMPLETE;
import static org.mule.runtime.core.context.notification.AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED;
import static org.mule.runtime.core.context.notification.PipelineMessageNotification.PROCESS_COMPLETE;
import static org.mule.runtime.core.context.notification.PipelineMessageNotification.PROCESS_END;
import static org.mule.runtime.core.context.notification.PipelineMessageNotification.PROCESS_START;
import static org.mule.tck.junit4.AbstractMuleContextTestCase.RECEIVE_TIMEOUT;

import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.TransformationService;
import org.mule.runtime.core.api.MessageContext;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.config.ChainedThreadingProfile;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.construct.AbstractPipeline;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.context.notification.ExceptionStrategyNotification;
import org.mule.runtime.core.context.notification.PipelineMessageNotification;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.core.management.stats.AllStatistics;
import org.mule.runtime.core.processor.ResponseMessageProcessorAdapter;
import org.mule.runtime.core.processor.strategy.AsynchronousProcessingStrategy;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.runtime.core.registry.DefaultRegistryBroker;
import org.mule.runtime.core.registry.MuleRegistryHelper;
import org.mule.runtime.core.transformer.simple.StringAppendTransformer;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.SensingNullReplyToHandler;
import org.mule.tck.TriggerableMessageSource;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

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
public class PipelineMessageNotificationTestCase extends AbstractMuleTestCase {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MuleContext muleContext;
  private MuleEvent event;
  private ServerNotificationManager notificationManager;
  private TestPipeline pipeline;
  private final String pipelineName = "testPipeline";

  private MessageContext context;

  @Before
  public void createMocks() throws Exception {

    when(muleContext.getStatistics()).thenReturn(new AllStatistics());
    when(muleContext.getConfiguration()).thenReturn(new DefaultMuleConfiguration());
    when(muleContext.getRegistry()).thenReturn(new MuleRegistryHelper(new DefaultRegistryBroker(muleContext), muleContext));
    when(muleContext.getDefaultThreadingProfile()).thenReturn(new ChainedThreadingProfile());
    notificationManager = mock(ServerNotificationManager.class);
    when(muleContext.getNotificationManager()).thenReturn(notificationManager);
    pipeline = new TestPipeline(pipelineName, muleContext);
    when(muleContext.getTransformationService()).thenReturn(new TransformationService(muleContext));

    context = DefaultMessageContext.create(pipeline, TEST_CONNECTOR);
  }

  @Test
  public void requestResponse() throws MuleException {
    TriggerableMessageSource source = new TriggerableMessageSource();
    pipeline.setMessageSource(source);
    pipeline.initialise();

    event = MuleEvent.builder(context).message(MuleMessage.builder().payload("request").build()).exchangePattern(REQUEST_RESPONSE)
        .flow(pipeline).build();

    source.trigger(event);

    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, event)));
    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_END, false, event)));
    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, false, event)));
    verify(notificationManager, times(3)).fireNotification(any(PipelineMessageNotification.class));
  }

  @Test
  public void requestResponseNonBlocking() throws Exception {
    assertRequestResponseNonBlockingWithMessageProcessor(new SensingNullMessageProcessor(), 1);
  }

  @Test
  public void requestResponseNonBlockingWithBlockingMessageProcessor() throws Exception {
    assertRequestResponseNonBlockingWithMessageProcessor(new StringAppendTransformer(""), 0);
  }

  private void assertRequestResponseNonBlockingWithMessageProcessor(MessageProcessor messageProcessor, int extraCompletes)
      throws Exception {
    TriggerableMessageSource source = new TriggerableMessageSource();
    pipeline.setMessageSource(source);
    pipeline.setProcessingStrategy(new NonBlockingProcessingStrategy());
    pipeline.setMessageProcessors(Collections.singletonList(messageProcessor));
    pipeline.initialise();

    SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();
    event = MuleEvent.builder(context).message(MuleMessage.builder().payload("request").build())
        .exchangePattern(REQUEST_RESPONSE).replyToHandler(nullReplyToHandler).flow(pipeline).build();
    source.trigger(event);

    new PollingProber(RECEIVE_TIMEOUT, 50).check(new JUnitLambdaProbe(() -> {
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, null)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_END, false, null)));
      verify(notificationManager, times(1 + extraCompletes))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, false, null)));
      verify(notificationManager, times(3 + extraCompletes)).fireNotification(any(PipelineMessageNotification.class));
      return true;
    }));
  }

  @Test
  public void oneWay() throws MuleException, InterruptedException {
    TriggerableMessageSource source = new TriggerableMessageSource();
    pipeline.setMessageSource(source);
    pipeline.initialise();

    event = MuleEvent.builder(context).message(MuleMessage.builder().payload("request").build()).exchangePattern(ONE_WAY)
        .flow(pipeline).build();

    source.trigger(event);

    new PollingProber(RECEIVE_TIMEOUT, 50).check(new JUnitLambdaProbe(() -> {
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, event)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_END, false, event)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, false, event)));
      verify(notificationManager, times(3)).fireNotification(any(PipelineMessageNotification.class));
      return true;
    }));
  }

  @Test
  public void requestResponseRequestException() throws MuleException, InterruptedException {
    TriggerableMessageSource source = new TriggerableMessageSource();
    pipeline.setMessageSource(source);
    pipeline.setExceptionListener(new DefaultMessagingExceptionStrategy());
    List<MessageProcessor> processors = new ArrayList<>();
    processors.add(new ExceptionThrowingMessageProcessor());
    pipeline.setMessageProcessors(processors);
    pipeline.initialise();

    event = MuleEvent.builder(context).message(MuleMessage.builder().payload("request").build()).exchangePattern(REQUEST_RESPONSE)
        .flow(pipeline).build();

    try {
      source.trigger(event);
    } catch (Exception e) {
    }

    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, event)));
    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, true, event)));
    verify(notificationManager, times(2)).fireNotification(any(PipelineMessageNotification.class));
  }

  @Test
  public void requestResponseNonBlockingRequestException() throws MuleException, InterruptedException {
    TriggerableMessageSource source = new TriggerableMessageSource();
    pipeline.setMessageSource(source);
    pipeline.setExceptionListener(new DefaultMessagingExceptionStrategy());
    List<MessageProcessor> processors = new ArrayList<>();
    processors.add(new ExceptionThrowingMessageProcessor());
    processors.add(new SensingNullMessageProcessor());
    pipeline.setMessageProcessors(processors);
    pipeline.setProcessingStrategy(new NonBlockingProcessingStrategy());
    pipeline.initialise();

    SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();
    event = MuleEvent.builder(context).message(MuleMessage.builder().payload("request").build())
        .exchangePattern(REQUEST_RESPONSE).replyToHandler(nullReplyToHandler).flow(pipeline).build();

    try {
      source.trigger(event);
    } catch (Exception e) {
    }

    new PollingProber(RECEIVE_TIMEOUT, 50).check(new JUnitLambdaProbe(() -> {
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, null)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, true, null)));
      verify(notificationManager, times(2)).fireNotification(any(PipelineMessageNotification.class));
      return true;
    }));
  }

  @Test
  public void requestResponseNonBlockingResponseException() throws MuleException, InterruptedException {
    TriggerableMessageSource source = new TriggerableMessageSource();
    pipeline.setMessageSource(source);
    pipeline.setExceptionListener(new DefaultMessagingExceptionStrategy());
    List<MessageProcessor> processors = new ArrayList<>();
    processors.add(new ResponseMessageProcessorAdapter(new ExceptionThrowingMessageProcessor()));
    processors.add(new SensingNullMessageProcessor());
    pipeline.setMessageProcessors(processors);
    pipeline.setProcessingStrategy(new NonBlockingProcessingStrategy());
    pipeline.initialise();

    SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();
    event = MuleEvent.builder(context).message(MuleMessage.builder().payload("request").build())
        .exchangePattern(REQUEST_RESPONSE).replyToHandler(nullReplyToHandler).flow(pipeline).build();

    try {
      source.trigger(event);
    } catch (Exception e) {
    }

    new PollingProber(RECEIVE_TIMEOUT, 50).check(new JUnitLambdaProbe(() -> {
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, null)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, false, null)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_END, false, null)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, true, null)));
      verify(notificationManager, times(4)).fireNotification(any(PipelineMessageNotification.class));
      return true;
    }));
  }

  @Test
  public void requestResponseResponseException() throws MuleException, InterruptedException {
    TriggerableMessageSource source = new TriggerableMessageSource();
    pipeline.setMessageSource(source);
    pipeline.setExceptionListener(new DefaultMessagingExceptionStrategy());
    List<MessageProcessor> processors = new ArrayList<>();
    processors.add(new ResponseMessageProcessorAdapter(new ExceptionThrowingMessageProcessor()));
    pipeline.setMessageProcessors(processors);
    pipeline.initialise();

    event = MuleEvent.builder(context).message(MuleMessage.builder().payload("request").build()).exchangePattern(REQUEST_RESPONSE)
        .flow(pipeline).build();

    try {
      source.trigger(event);
    } catch (Exception e) {
    }
    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, event)));
    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_END, false, event)));
    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, true, event)));
    verify(notificationManager, times(3)).fireNotification(any(PipelineMessageNotification.class));
  }

  @Test
  public void oneWayRequestException() throws MuleException, InterruptedException {
    TriggerableMessageSource source = new TriggerableMessageSource();
    pipeline.setMessageSource(source);
    pipeline.setExceptionListener(new DefaultMessagingExceptionStrategy());
    List<MessageProcessor> processors = new ArrayList<>();
    processors.add(new ExceptionThrowingMessageProcessor());
    pipeline.setMessageProcessors(processors);
    pipeline.initialise();

    event = MuleEvent.builder(context).message(MuleMessage.builder().payload("request").build()).exchangePattern(ONE_WAY)
        .flow(pipeline).build();

    try {
      source.trigger(event);
    } catch (Exception e) {
    }
    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, event)));
    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, true, null)));
    verify(notificationManager, times(2)).fireNotification(any(PipelineMessageNotification.class));
  }

  @Test
  public void oneWayAsyncRequestException() throws MuleException, InterruptedException {
    TriggerableMessageSource source = new TriggerableMessageSource();
    Flow pipeline = new Flow("test", muleContext);
    pipeline.setProcessingStrategy(new AsynchronousProcessingStrategy());
    final CountDownLatch latch = new CountDownLatch(1);
    pipeline.setMessageSource(source);
    pipeline.setExceptionListener(new DefaultMessagingExceptionStrategy());
    List<MessageProcessor> processors = new ArrayList<>();
    processors.add(event -> {
      latch.countDown();
      throw new RuntimeException("error");
    });
    pipeline.setMessageProcessors(processors);
    pipeline.initialise();
    pipeline.start();

    event = MuleEvent.builder(context).message(MuleMessage.builder().payload("request").build()).exchangePattern(ONE_WAY)
        .flow(pipeline).build();

    source.trigger(event);
    latch.await(AbstractMuleContextTestCase.RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);

    new PollingProber(2000, 50).check(new JUnitLambdaProbe(() -> {
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, event)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_ASYNC_SCHEDULED, false, event)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, false, null)));
      // Event is not same, because it's copied
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_ASYNC_COMPLETE, true, null)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(ExceptionStrategyNotification.PROCESS_START,
                                                                                    false, null)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(ExceptionStrategyNotification.PROCESS_END,
                                                                                    false, null)));
      verify(notificationManager, times(6)).fireNotification(any(PipelineMessageNotification.class));
      return true;
    }));

    pipeline.stop();
    pipeline.dispose();
  }


  private class TestPipeline extends AbstractPipeline {

    CountDownLatch latch = new CountDownLatch(2);

    public TestPipeline(String name, MuleContext muleContext) {
      super(name, muleContext);
    }

    @Override
    protected void configureMessageProcessors(MessageProcessorChainBuilder builder) throws MuleException {
      builder.chain((MessageProcessor) event -> {
        latch.countDown();
        return event;
      });
      super.configureMessageProcessors(builder);
    }

    @Override
    protected void configurePostProcessors(MessageProcessorChainBuilder builder) throws MuleException {
      super.configurePostProcessors(builder);
      builder.chain((MessageProcessor) event -> {
        latch.countDown();
        return event;
      });
    }

    @Override
    public String getConstructType() {
      return "test";
    }
  }

  private class PipelineMessageNotificiationArgumentMatcher extends ArgumentMatcher<PipelineMessageNotification> {

    private int expectedAction;
    private boolean exceptionExpected;
    private MuleEvent event;

    public PipelineMessageNotificiationArgumentMatcher(int expectedAction, boolean exceptionExpected, MuleEvent event) {
      this.expectedAction = expectedAction;
      this.exceptionExpected = exceptionExpected;
      this.event = event;
    }

    @Override
    public boolean matches(Object argument) {
      ServerNotification notification = (ServerNotification) argument;
      MessagingException exception = null;
      if (notification instanceof PipelineMessageNotification) {
        exception = ((PipelineMessageNotification) notification).getException();
      } else if (notification instanceof AsyncMessageNotification) {
        exception = ((AsyncMessageNotification) notification).getException();
      }

      if (exceptionExpected) {
        return expectedAction == notification.getAction() && exception != null && notification.getSource() != null
            && (this.event == null || this.event == notification.getSource());
      } else {
        return expectedAction == notification.getAction() && exception == null && notification.getSource() != null
            && (this.event == null || this.event == notification.getSource());
      }

    }
  }

  public static class ExceptionThrowingMessageProcessor implements MessageProcessor {

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      throw new IllegalStateException();
    }
  }

}
