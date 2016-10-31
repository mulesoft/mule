/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.construct.processor;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.rules.ExpectedException.none;
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
import static org.mule.tck.MuleTestUtils.processAsStreamAndBlock;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.TransformationService;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.strategy.factory.AsynchronousProcessingStrategyFactory;
import org.mule.runtime.core.config.ChainedThreadingProfile;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.construct.AbstractPipeline;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.context.notification.ExceptionStrategyNotification;
import org.mule.runtime.core.context.notification.PipelineMessageNotification;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.management.stats.AllStatistics;
import org.mule.runtime.core.processor.ResponseMessageProcessorAdapter;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatcher;
import org.reactivestreams.Publisher;

@RunWith(Parameterized.class)
public class PipelineMessageNotificationTestCase extends AbstractReactiveProcessorTestCase {

  private Event event;
  private ServerNotificationManager notificationManager;
  private TestPipeline pipeline;
  private final String pipelineName = "testPipeline";

  private EventContext context;

  @Rule
  public ExpectedException thrown = none();

  public PipelineMessageNotificationTestCase(boolean reactive) {
    super(reactive);
  }

  @Before
  public void createMocks() throws Exception {
    muleContext = mockContextWithServices();
    when(muleContext.getStatistics()).thenReturn(new AllStatistics());
    when(muleContext.getConfiguration()).thenReturn(new DefaultMuleConfiguration());
    when(muleContext.getDefaultThreadingProfile()).thenReturn(new ChainedThreadingProfile());
    notificationManager = mock(ServerNotificationManager.class);
    when(muleContext.getNotificationManager()).thenReturn(notificationManager);
    pipeline = new TestPipeline(pipelineName, muleContext);
    when(muleContext.getTransformationService()).thenReturn(new TransformationService(muleContext));
    context = DefaultEventContext.create(pipeline, TEST_CONNECTOR);
  }

  @Test
  public void requestResponse() throws Exception {
    pipeline.initialise();

    event = Event.builder(context).message(InternalMessage.builder().payload("request").build()).exchangePattern(REQUEST_RESPONSE)
        .flow(pipeline).build();

    process(pipeline, event);

    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, event)));
    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_END, false, event)));
    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, false, event)));
    verify(notificationManager, times(3)).fireNotification(any(PipelineMessageNotification.class));
  }

  @Test
  public void oneWay() throws Exception {
    pipeline.initialise();

    event = Event.builder(context).message(InternalMessage.builder().payload("request").build()).exchangePattern(ONE_WAY)
        .flow(pipeline).build();

    process(pipeline, event);

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
  public void requestResponseRequestException() throws Exception {
    pipeline.setExceptionListener(new DefaultMessagingExceptionStrategy());
    List<Processor> processors = new ArrayList<>();
    processors.add(new ExceptionThrowingMessageProcessor());
    pipeline.setMessageProcessors(processors);
    pipeline.initialise();

    event = Event.builder(context).message(InternalMessage.builder().payload("request").build()).exchangePattern(REQUEST_RESPONSE)
        .flow(pipeline).build();

    thrown.expect(instanceOf(MessagingException.class));
    thrown.expectCause(instanceOf(IllegalStateException.class));
    try {
      process(pipeline, event);
    } finally {
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, event)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, true, event)));
      verify(notificationManager, times(2)).fireNotification(any(PipelineMessageNotification.class));
    }
  }

  @Test
  public void requestResponseResponseException() throws Exception {
    pipeline.setExceptionListener(new DefaultMessagingExceptionStrategy());
    List<Processor> processors = new ArrayList<>();
    processors.add(new ResponseMessageProcessorAdapter(new ExceptionThrowingMessageProcessor()));
    pipeline.setMessageProcessors(processors);
    pipeline.initialise();

    event = Event.builder(context).message(InternalMessage.builder().payload("request").build()).exchangePattern(REQUEST_RESPONSE)
        .flow(pipeline).build();

    thrown.expect(instanceOf(MessagingException.class));
    thrown.expectCause(instanceOf(IllegalStateException.class));
    try {
      process(pipeline, event);
    } finally {
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, event)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_END, false, event)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, true, event)));
      verify(notificationManager, times(3)).fireNotification(any(PipelineMessageNotification.class));
    }
  }

  @Test
  public void oneWayRequestException() throws Exception {
    pipeline.setExceptionListener(new DefaultMessagingExceptionStrategy());
    List<Processor> processors = new ArrayList<>();
    processors.add(new ExceptionThrowingMessageProcessor());
    pipeline.setMessageProcessors(processors);
    pipeline.initialise();

    event = Event.builder(context).message(InternalMessage.builder().payload("request").build()).exchangePattern(ONE_WAY)
        .flow(pipeline).build();

    thrown.expect(instanceOf(MessagingException.class));
    thrown.expectCause(is(instanceOf(IllegalStateException.class)));
    try {
      process(pipeline, event);
    } finally {
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, event)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, true, null)));
      verify(notificationManager, times(2)).fireNotification(any(PipelineMessageNotification.class));
    }
  }

  @Test
  public void oneWayAsyncRequestException() throws Exception {
    Flow pipeline = new Flow("test", muleContext);
    pipeline.setProcessingStrategyFactory(new AsynchronousProcessingStrategyFactory());
    pipeline.setExceptionListener(new DefaultMessagingExceptionStrategy());
    List<Processor> processors = new ArrayList<>();
    processors.add(event -> {
      throw new RuntimeException("error");
    });
    pipeline.setMessageProcessors(processors);
    pipeline.initialise();
    pipeline.start();

    event = Event.builder(context).message(InternalMessage.builder().payload("request").build()).exchangePattern(ONE_WAY)
        .flow(pipeline).build();

    try {
      process(pipeline, event);
    } finally {
      new PollingProber(2000, 50).check(new JUnitLambdaProbe(() -> {
        verify(notificationManager, times(1))
            .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, event)));
        verify(notificationManager, times(1))
            .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, false, null)));
        verify(notificationManager, times(1))
            .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(ExceptionStrategyNotification.PROCESS_END,
                                                                                      false, null)));
        verify(notificationManager, times(6)).fireNotification(any(PipelineMessageNotification.class));
        verify(notificationManager, times(1))
            .fireNotification(
                              argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_ASYNC_SCHEDULED, false, event)));
        verify(notificationManager, times(1))
            .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_ASYNC_COMPLETE, true, null)));
        return true;
      }));
    }
    pipeline.stop();
    pipeline.dispose();
  }


  private class TestPipeline extends AbstractPipeline implements Processor {

    CountDownLatch latch = new CountDownLatch(2);

    public TestPipeline(String name, MuleContext muleContext) {
      super(name, muleContext);
    }

    @Override
    protected void configureMessageProcessors(MessageProcessorChainBuilder builder) throws MuleException {
      builder.chain((Processor) event -> {
        latch.countDown();
        return event;
      });
      super.configureMessageProcessors(builder);
    }

    @Override
    protected void configurePostProcessors(MessageProcessorChainBuilder builder) throws MuleException {
      super.configurePostProcessors(builder);
      builder.chain((Processor) event -> {
        latch.countDown();
        return event;
      });
    }

    @Override
    public String getConstructType() {
      return "test";
    }

    @Override
    public Event process(Event event) throws MuleException {
      return pipeline.process(event);
    }

    @Override
    public Publisher<Event> apply(Publisher<Event> publisher) {
      return from(publisher).transform(pipeline);
    }
  }

  private class PipelineMessageNotificiationArgumentMatcher extends ArgumentMatcher<PipelineMessageNotification> {

    private int expectedAction;
    private boolean exceptionExpected;
    private Event event;

    public PipelineMessageNotificiationArgumentMatcher(int expectedAction, boolean exceptionExpected, Event event) {
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
            && (this.event == null || this.event.getMessage().equals(((Event) notification.getSource()).getMessage()));
      } else {
        return expectedAction == notification.getAction() && exception == null && notification.getSource() != null
            && (this.event == null || this.event.getMessage().equals(((Event) notification.getSource()).getMessage()));
      }
    }
  }

  public static class ExceptionThrowingMessageProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      throw new IllegalStateException();
    }
  }

  /*
   * Override to not unwrap MessagingExceptions
   */
  @Override
  protected Event process(Processor processor, Event event) throws MuleException {
    if (isReactive()) {
      return processAsStreamAndBlock(event, processor);
    } else {
      return processor.process(event);
    }
  }

}
