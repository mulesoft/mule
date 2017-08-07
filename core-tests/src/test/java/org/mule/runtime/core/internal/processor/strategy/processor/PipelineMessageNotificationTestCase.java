/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy.processor;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.context.notification.PipelineMessageNotification.PROCESS_COMPLETE;
import static org.mule.runtime.core.api.context.notification.PipelineMessageNotification.PROCESS_END;
import static org.mule.runtime.core.api.context.notification.PipelineMessageNotification.PROCESS_START;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.UNKNOWN;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory.DEFAULT_MAX_CONCURRENCY;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.DefaultTransformationService;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.api.context.notification.EnrichedServerNotification;
import org.mule.runtime.core.api.context.notification.ErrorHandlerNotification;
import org.mule.runtime.core.api.context.notification.PipelineMessageNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.exception.ErrorTypeLocator;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.management.stats.AllStatistics;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.construct.DefaultFlowBuilder.DefaultFlow;
import org.mule.runtime.core.internal.exception.ErrorHandler;
import org.mule.runtime.core.internal.exception.ErrorHandlerFactory;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;

import java.util.List;
import java.util.Optional;
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

  public PipelineMessageNotificationTestCase(Mode mode) {
    super(mode);
  }

  @Before
  public void createMocks() throws Exception {
    muleContext.dispose();
    muleContext = mockContextWithServices();
    when(muleContext.getStatistics()).thenReturn(new AllStatistics());
    when(muleContext.getConfiguration()).thenReturn(new DefaultMuleConfiguration());
    when(muleContext.getDefaultErrorHandler()).thenReturn(new ErrorHandlerFactory().createDefault());
    notificationManager = mock(ServerNotificationManager.class);
    when(muleContext.getNotificationManager()).thenReturn(notificationManager);
    ErrorTypeLocator errorTypeLocator = mock(ErrorTypeLocator.class);
    ErrorType errorType = mock(ErrorType.class);
    when(errorTypeLocator.lookupErrorType(any(Throwable.class))).thenReturn(errorType);
    when(errorTypeLocator.<String, Throwable>lookupComponentErrorType(any(ComponentIdentifier.class),
                                                                      any(Throwable.class)))
                                                                          .thenReturn(errorType);
    when(muleContext.getErrorTypeLocator()).thenReturn(errorTypeLocator);
    when(muleContext.getErrorTypeRepository().getErrorType(UNKNOWN)).thenReturn(Optional.of(mock(ErrorType.class)));
    when(muleContext.getTransformationService()).thenReturn(new DefaultTransformationService(muleContext));
  }

  public void createTestPipeline(List<Processor> processors, ErrorHandler errorHandler) {
    pipeline = new TestPipeline(pipelineName, muleContext, null, processors, errorHandler);
    context = DefaultEventContext.create(pipeline, TEST_CONNECTOR_LOCATION);
  }

  @After
  public void after() throws MuleException {
    stopIfNeeded(pipeline);
    stopIfNeeded(muleContext.getSchedulerService());
  }

  @Test
  public void send() throws Exception {
    createTestPipeline(emptyList(), null);

    pipeline.initialise();
    pipeline.start();

    event = Event.builder(context).message(of("request")).flow(pipeline).build();

    process(pipeline, event);

    verifySucess();
  }

  @Test
  public void requestResponseException() throws Exception {
    createTestPipeline(singletonList(new ExceptionThrowingMessageProcessor()), new ErrorHandlerFactory().createDefault());

    pipeline.initialise();
    pipeline.start();

    event = Event.builder(context).message(of("request")).flow(pipeline).build();

    thrown.expect(instanceOf(MessagingException.class));
    thrown.expectCause(instanceOf(IllegalStateException.class));
    try {
      process(pipeline, event);
    } finally {
      verifyException();
    }
  }

  private void verifySucess() {
    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, event)));
    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_END, false, event)));
    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, false, event)));
    verify(notificationManager, times(3)).fireNotification(any(PipelineMessageNotification.class));
  }

  private void verifyException() {
    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, event)));
    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, true, null)));
    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(ErrorHandlerNotification.PROCESS_START,
                                                                                  false, null)));
    verify(notificationManager, times(1))
        .fireNotification(argThat(new PipelineMessageNotificiationArgumentMatcher(ErrorHandlerNotification.PROCESS_END,
                                                                                  false, null)));
    verify(notificationManager, times(4)).fireNotification(any(PipelineMessageNotification.class));
  }

  private class TestPipeline extends DefaultFlow {

    CountDownLatch latch = new CountDownLatch(2);

    public TestPipeline(String name, MuleContext muleContext, MessageSource messageSource, List<Processor> messageProcessors,
                        ErrorHandler errorHandler) {
      super(name, muleContext, messageSource, messageProcessors, ofNullable(errorHandler), empty(), INITIAL_STATE_STARTED,
            DEFAULT_MAX_CONCURRENCY, createFlowStatistics(name, muleContext));
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
      EnrichedServerNotification notification = (EnrichedServerNotification) argument;
      MessagingException exception = null;
      if (notification instanceof PipelineMessageNotification) {
        exception = ((PipelineMessageNotification) notification).getException();
      } else if (notification instanceof AsyncMessageNotification) {
        exception = ((AsyncMessageNotification) notification).getException();
      }

      if (exceptionExpected) {
        return expectedAction == notification.getAction() && exception != null && notification.getEvent() != null
            && notification.getEvent().getMessage() != null
            && (this.event == null || this.event.getMessage().equals(notification.getEvent().getMessage()));
      } else {
        return expectedAction == notification.getAction() && exception == null && notification.getEvent() != null
            && notification.getEvent().getMessage() != null
            && (this.event == null || this.event.getMessage().equals(notification.getEvent().getMessage()));
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
