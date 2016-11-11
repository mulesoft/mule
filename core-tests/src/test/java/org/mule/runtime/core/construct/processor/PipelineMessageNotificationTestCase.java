/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.construct.processor;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.context.notification.AsyncMessageNotification.PROCESS_ASYNC_COMPLETE;
import static org.mule.runtime.core.context.notification.AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED;
import static org.mule.runtime.core.context.notification.PipelineMessageNotification.PROCESS_COMPLETE;
import static org.mule.runtime.core.context.notification.PipelineMessageNotification.PROCESS_END;
import static org.mule.runtime.core.context.notification.PipelineMessageNotification.PROCESS_START;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.TransformationService;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.context.notification.ExceptionStrategyNotification;
import org.mule.runtime.core.context.notification.PipelineMessageNotification;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.core.exception.ErrorTypeLocator;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.management.stats.AllStatistics;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatcher;

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
    muleContext.dispose();
    muleContext = mockContextWithServices();
    when(muleContext.getStatistics()).thenReturn(new AllStatistics());
    when(muleContext.getConfiguration()).thenReturn(new DefaultMuleConfiguration());
    when(muleContext.getDefaultErrorHandler()).thenReturn(new DefaultMessagingExceptionStrategy(muleContext));
    notificationManager = mock(ServerNotificationManager.class);
    when(muleContext.getNotificationManager()).thenReturn(notificationManager);
    ErrorTypeLocator errorTypeLocator = mock(ErrorTypeLocator.class);
    ErrorType errorType = mock(ErrorType.class);
    when(errorTypeLocator.lookupErrorType(any(Throwable.class))).thenReturn(errorType);
    when(errorTypeLocator.<String, Throwable>lookupComponentErrorType(any(ComponentIdentifier.class), any(Throwable.class)))
        .thenReturn(errorType);
    when(muleContext.getErrorTypeLocator()).thenReturn(errorTypeLocator);
    pipeline = new TestPipeline(pipelineName, muleContext);
    when(muleContext.getTransformationService()).thenReturn(new TransformationService(muleContext));
    context = DefaultEventContext.create(pipeline, TEST_CONNECTOR);
  }

  @After
  public void after() throws MuleException {
    stopIfNeeded(muleContext.getSchedulerService());
  }

  @Test
  public void requestResponse() throws Exception {
    pipeline.initialise();
    pipeline.start();

    event = Event.builder(context).message(InternalMessage.builder().payload("request").build()).exchangePattern(REQUEST_RESPONSE)
        .flow(pipeline).build();

    processFlow(pipeline, event);

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
    pipeline.start();

    event = Event.builder(context).message(InternalMessage.builder().payload("request").build()).exchangePattern(ONE_WAY)
        .flow(pipeline).build();

    processFlow(pipeline, event);

    new PollingProber(RECEIVE_TIMEOUT, 50).check(new JUnitLambdaProbe(() -> {
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, event)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_END, false, event)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, false, event)));
      verify(notificationManager, times(5)).fireNotification(any(PipelineMessageNotification.class));
      verify(notificationManager, times(1))
          .fireNotification(
                            argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_ASYNC_SCHEDULED, false, event)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_ASYNC_COMPLETE, false, null)));
      return true;
    }));
  }

  @Test
  public void requestResponseException() throws Exception {
    pipeline.setExceptionListener(new DefaultMessagingExceptionStrategy());
    List<Processor> processors = new ArrayList<>();
    processors.add(new ExceptionThrowingMessageProcessor());
    pipeline.setMessageProcessors(processors);
    pipeline.initialise();
    pipeline.start();

    event = Event.builder(context).message(InternalMessage.builder().payload("request").build()).exchangePattern(REQUEST_RESPONSE)
        .flow(pipeline).build();

    thrown.expect(instanceOf(MessagingException.class));
    thrown.expectCause(instanceOf(IllegalStateException.class));
    try {
      processFlow(pipeline, event);
    } finally {
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, event)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, true, event)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(ExceptionStrategyNotification.PROCESS_START,
                                                                                    false, null)));
      verify(notificationManager, times(1))
          .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(ExceptionStrategyNotification.PROCESS_END,
                                                                                    false, null)));
      verify(notificationManager, times(4)).fireNotification(any(PipelineMessageNotification.class));
    }
  }

  @Test
  public void oneWayException() throws Exception {
    Flow pipeline = new Flow("test", muleContext);
    pipeline.setExceptionListener(new DefaultMessagingExceptionStrategy());
    List<Processor> processors = new ArrayList<>();
    processors.add(new ExceptionThrowingMessageProcessor());
    pipeline.setMessageProcessors(processors);
    pipeline.initialise();
    pipeline.start();

    event = Event.builder(context).message(InternalMessage.builder().payload("request").build()).exchangePattern(ONE_WAY)
        .flow(pipeline).build();

    try {
      processFlow(pipeline, event);
    } finally {
      new PollingProber(RECEIVE_TIMEOUT, 50).check(new JUnitLambdaProbe(() -> {
        verify(notificationManager, times(1))
            .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, event)));
        verify(notificationManager, times(1))
            .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, false, null)));
        verify(notificationManager, times(1))
            .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(ExceptionStrategyNotification.PROCESS_START,
                                                                                      false, null)));
        verify(notificationManager, times(1))
            .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(ExceptionStrategyNotification.PROCESS_END,
                                                                                      false, null)));
        verify(notificationManager, times(1))
            .fireNotification(
                              argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_ASYNC_SCHEDULED, false, event)));
        verify(notificationManager, times(1))
            .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_ASYNC_COMPLETE, true, null)));
        verify(notificationManager, times(6)).fireNotification(any(PipelineMessageNotification.class));
        return true;
      }));
    }
    pipeline.stop();
    pipeline.dispose();
  }

  private class TestPipeline extends Flow {

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

}
