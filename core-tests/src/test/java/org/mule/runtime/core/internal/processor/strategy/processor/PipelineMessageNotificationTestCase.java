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
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_COMPLETE;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_END;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_START;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory.DEFAULT_MAX_CONCURRENCY;
import static org.mule.tck.util.MuleContextUtils.getNotificationDispatcher;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.notification.EnrichedServerNotification;
import org.mule.runtime.api.notification.ErrorHandlerNotification;
import org.mule.runtime.api.notification.FlowConstructNotification;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.management.stats.AllStatistics;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.construct.DefaultFlowBuilder.DefaultFlow;
import org.mule.runtime.core.internal.exception.ErrorHandler;
import org.mule.runtime.core.internal.exception.ErrorHandlerFactory;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.core.privileged.processor.InternalProcessor;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.transformer.ExtendedTransformationService;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

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

  private CoreEvent event;
  private NotificationDispatcher notificationFirer;
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
    notificationFirer = getNotificationDispatcher(muleContext);
    when(muleContext.getDefaultErrorHandler(empty())).thenReturn(new ErrorHandlerFactory().createDefault(notificationFirer));
    mockErrorTypeLocator();
    when(muleContext.getTransformationService()).thenReturn(new ExtendedTransformationService(muleContext));
  }

  private void mockErrorTypeLocator() {
    ErrorTypeLocator typeLocator = ((PrivilegedMuleContext) muleContext).getErrorTypeLocator();
    ErrorType errorType = mock(ErrorType.class);
    when(errorType.getIdentifier()).thenReturn("ID");
    when(errorType.getNamespace()).thenReturn("NS");
    when(typeLocator.lookupErrorType(any(Throwable.class))).thenReturn(errorType);
    when(typeLocator.<String, Throwable>lookupComponentErrorType(any(ComponentIdentifier.class), any(Throwable.class)))
        .thenReturn(errorType);
  }

  public void createTestPipeline(List<Processor> processors, ErrorHandler errorHandler) {
    pipeline = new TestPipeline(pipelineName, muleContext, null, processors, errorHandler);
    context = create(pipeline, TEST_CONNECTOR_LOCATION);
  }

  @After
  public void after() throws MuleException {
    stopIfNeeded(pipeline);
    disposeIfNeeded(pipeline, getLogger(getClass()));
    stopIfNeeded(muleContext.getSchedulerService());
  }

  @Test
  public void send() throws Exception {
    createTestPipeline(emptyList(), null);

    pipeline.initialise();
    pipeline.start();

    event = InternalEvent.builder(context).message(of("request")).build();

    process(pipeline, event);

    switch (mode) {
      case BLOCKING:
        new PollingProber().check(new JUnitLambdaProbe(() -> {
          verifySucess();
          return true;
        }));
        break;
      case NON_BLOCKING:
        new PollingProber().check(new JUnitLambdaProbe(() -> {
          verifySucess();
          return true;
        }));
        break;
      default:
        fail();
    }
  }

  @Test
  public void requestResponseException() throws Exception {
    createTestPipeline(singletonList(new ExceptionThrowingMessageProcessor()),
                       new ErrorHandlerFactory().createDefault(notificationFirer));

    pipeline.initialise();
    pipeline.start();

    event = InternalEvent.builder(context).message(of("request")).build();

    thrown.expect(instanceOf(MessagingException.class));
    thrown.expectCause(instanceOf(IllegalStateException.class));
    try {
      process(pipeline, event);
    } finally {
      switch (mode) {
        case BLOCKING:
          new PollingProber().check(new JUnitLambdaProbe(() -> {
            verifyException();
            return true;
          }));
          break;
        case NON_BLOCKING:
          new PollingProber().check(new JUnitLambdaProbe(() -> {
            verifyException();
            return true;
          }));
          break;
        default:
          fail();
      }
    }
  }

  private void verifySucess() {
    verify(notificationFirer, times(2)).dispatch(any(FlowConstructNotification.class));

    verify(notificationFirer, times(1))
        .dispatch(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, event)));
    verify(notificationFirer, times(1))
        .dispatch(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_END, false, event)));
    verify(notificationFirer, times(1))
        .dispatch(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, false, event)));
    verify(notificationFirer, times(3)).dispatch(any(PipelineMessageNotification.class));
  }

  private void verifyException() {
    verify(notificationFirer, times(2)).dispatch(any(FlowConstructNotification.class));
    verify(notificationFirer, times(2)).dispatch(any(PipelineMessageNotification.class));
    verify(notificationFirer, times(2)).dispatch(any(ErrorHandlerNotification.class));

    verify(notificationFirer, times(1))
        .dispatch(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_START, false, event)));
    verify(notificationFirer, times(1))
        .dispatch(argThat(new PipelineMessageNotificiationArgumentMatcher(PROCESS_COMPLETE, true, event)));
    verify(notificationFirer, times(1))
        .dispatch(argThat(new PipelineMessageNotificiationArgumentMatcher(ErrorHandlerNotification.PROCESS_START, true, event)));
    verify(notificationFirer, times(1))
        .dispatch(argThat(new PipelineMessageNotificiationArgumentMatcher(ErrorHandlerNotification.PROCESS_END, true, event)));
  }

  private class TestPipeline extends DefaultFlow {

    CountDownLatch latch = new CountDownLatch(2);

    public TestPipeline(String name, MuleContext muleContext, MessageSource messageSource, List<Processor> messageProcessors,
                        ErrorHandler errorHandler) {
      super(name, muleContext, messageSource, messageProcessors, ofNullable(errorHandler), empty(), INITIAL_STATE_STARTED,
            DEFAULT_MAX_CONCURRENCY, createFlowStatistics(name, muleContext), new ComponentInitialStateManager() {

              @Override
              public boolean mustStartMessageSource(Component component) {
                return true;
              }
            });
    }

    @Override
    protected void configureMessageProcessors(MessageProcessorChainBuilder builder) throws MuleException {
      builder.chain((Processor) event -> {
        latch.countDown();
        return event;
      });
      super.configureMessageProcessors(builder);
      builder.chain((Processor) event -> {
        latch.countDown();
        return event;
      });
    }

  }

  private class PipelineMessageNotificiationArgumentMatcher implements ArgumentMatcher<Notification> {

    private final int expectedAction;
    private final boolean exceptionExpected;
    private final CoreEvent event;

    public PipelineMessageNotificiationArgumentMatcher(int expectedAction, boolean exceptionExpected, CoreEvent event) {
      this.expectedAction = expectedAction;
      this.exceptionExpected = exceptionExpected;
      this.event = event;
    }

    @Override
    public boolean matches(Notification argument) {
      if (!(argument instanceof PipelineMessageNotification || argument instanceof ErrorHandlerNotification)) {
        return false;
      }

      EnrichedServerNotification notification = (EnrichedServerNotification) argument;
      Exception exception = notification.getException();

      boolean result =
          expectedAction == notification.getAction().getActionId()
              && (notification.getEvent() == null) == (this.event == null)
              && (this.event == null ||
                  (this.event.getMessage().getPayload().equals(notification.getEvent().getMessage().getPayload()) &&
                      this.event.getMessage().getAttributes().equals(notification.getEvent().getMessage().getAttributes())))
              && exceptionExpected == (exception != null);

      return result;
    }
  }

  public static class ExceptionThrowingMessageProcessor implements Processor, InternalProcessor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      throw new IllegalStateException();
    }
  }
}
