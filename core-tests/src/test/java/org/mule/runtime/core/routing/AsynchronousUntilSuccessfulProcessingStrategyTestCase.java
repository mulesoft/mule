/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.routing.UntilSuccessful.PROCESS_ATTEMPT_COUNT_PROPERTY_NAME;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.TransformationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.exception.ErrorTypeLocator;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.retry.RetryPolicyExhaustedException;
import org.mule.runtime.core.routing.filters.ExpressionFilter;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.core.util.store.SimpleMemoryObjectStore;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class AsynchronousUntilSuccessfulProcessingStrategyTestCase extends AbstractMuleTestCase {

  private static interface FailCallback {

    void doFail() throws Exception;
  }

  private static final String EXPECTED_FAILURE_MSG = "expected failure";
  private static final int DEFAULT_RETRIES = 4;
  private static final int DEFAULT_TRIES = DEFAULT_RETRIES + 1;

  private final Latch exceptionHandlingLatch = new Latch();
  private UntilSuccessfulConfiguration mockUntilSuccessfulConfiguration =
      mock(UntilSuccessfulConfiguration.class, RETURNS_DEEP_STUBS.get());
  private FlowConstruct mockFlow = mock(FlowConstruct.class, RETURNS_DEEP_STUBS.get());
  private MuleEvent mockEvent = mock(MuleEvent.class, RETURNS_DEEP_STUBS.get());
  private MessageProcessor mockRoute = mock(MessageProcessor.class, RETURNS_DEEP_STUBS.get());
  private ExpressionFilter mockAlwaysTrueFailureExpressionFilter = mock(ExpressionFilter.class, RETURNS_DEEP_STUBS.get());
  private ThreadPoolExecutor mockPool = mock(ThreadPoolExecutor.class, RETURNS_DEEP_STUBS.get());
  private ScheduledThreadPoolExecutor mockScheduledPool = mock(ScheduledThreadPoolExecutor.class, RETURNS_DEEP_STUBS.get());
  private SimpleMemoryObjectStore<MuleEvent> objectStore = new SimpleMemoryObjectStore<>();
  private MessageProcessor mockDLQ = mock(MessageProcessor.class);
  private FailCallback failRoute = () -> {
  };
  private CountDownLatch routeCountDownLatch;
  @Mock
  private MuleContext muleContext;
  @Mock
  private TransformationService transformationService;

  @Before
  public void setUp() throws Exception {
    when(mockAlwaysTrueFailureExpressionFilter.accept(any(MuleEvent.class))).thenReturn(true);
    when(mockUntilSuccessfulConfiguration.getRoute()).thenReturn(mockRoute);
    when(mockUntilSuccessfulConfiguration.getAckExpression()).thenReturn(null);
    when(mockUntilSuccessfulConfiguration.getMaxRetries()).thenReturn(DEFAULT_RETRIES);
    final MuleMessage mockMessage = MuleMessage.builder().payload("").build();
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockEvent.getFlowVariable(PROCESS_ATTEMPT_COUNT_PROPERTY_NAME)).thenAnswer(new Answer<Object>() {

      private int numberOfAttempts = 0;

      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        return numberOfAttempts++;
      }
    });
    when(mockUntilSuccessfulConfiguration.getThreadingProfile().createPool(anyString())).thenReturn(mockPool);
    when(mockUntilSuccessfulConfiguration.createScheduledRetriesPool(anyString())).thenReturn(mockScheduledPool);
    when(mockUntilSuccessfulConfiguration.getObjectStore()).thenReturn(objectStore);
    objectStore.clear();
    configureMockPoolToInvokeRunnableInNewThread();
    configureMockScheduledPoolToInvokeRunnableInNewThread();
    configureMockRouteToCountDownRouteLatch();
    configureExceptionStrategyToReleaseLatchWhenExecuted();
    configureDLQToReleaseLatchWhenExecuted();
    when(muleContext.getTransformationService()).thenReturn(transformationService);
    when(muleContext.getErrorTypeLocator()).thenReturn(mock(ErrorTypeLocator.class, RETURNS_DEEP_STUBS.get()));
    when(transformationService.transform(any(MuleMessage.class), any(DataType.class))).thenAnswer(
                                                                                                  invocation -> MuleMessage
                                                                                                      .builder()
                                                                                                      .payload(invocation
                                                                                                          .getArguments()[0]
                                                                                                              .toString()
                                                                                                              .getBytes())
                                                                                                      .build());
  }

  @Test(expected = InitialisationException.class)
  public void failWhenObjectStoreIsNull() throws Exception {
    when(mockUntilSuccessfulConfiguration.getObjectStore()).thenReturn(null);
    createProcessingStrategy();
  }

  @Test
  public void alwaysFail() throws Exception {
    executeUntilSuccessfulFailingRoute(() -> {
      throw new RuntimeException(EXPECTED_FAILURE_MSG);
    });
    waitUntilRouteIsExecuted();
  }

  @Test
  public void alwaysFailUsingFailureExpression() throws Exception {
    when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(null);
    when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
    executeUntilSuccessfulFailingRoute(() -> {
      throw new RuntimeException(EXPECTED_FAILURE_MSG);
    });
    waitUntilRouteIsExecuted();
    waitUntilExceptionIsHandled();

    verify(mockFlow.getExceptionListener(), times(1)).handleException(argThat(new ArgumentMatcher<MessagingException>() {

      @Override
      public boolean matches(Object item) {
        return item instanceof MessagingException
            && ((MessagingException) item).getCause() instanceof RetryPolicyExhaustedException
            && EXPECTED_FAILURE_MSG.equals(((MessagingException) item).getCauseException().getMessage());
      }
    }), eq(mockEvent));
    verify(mockDLQ, never()).process(any(MuleEvent.class));
  }

  @Test
  public void alwaysFailMessageUsingFailureExpression() throws Exception {
    when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(null);
    when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
    executeUntilSuccessfulFailingRoute(() -> {
      throw new MessagingException(CoreMessages.createStaticMessage(EXPECTED_FAILURE_MSG), mockEvent, mockRoute);
    });
    waitUntilRouteIsExecuted();
    waitUntilExceptionIsHandled();

    verify(mockFlow.getExceptionListener(), times(1)).handleException(argThat(new ArgumentMatcher<MessagingException>() {

      @Override
      public boolean matches(Object item) {
        return item instanceof MessagingException
            && ((MessagingException) item).getCauseException() instanceof RetryPolicyExhaustedException
            && ((MessagingException) item).getCauseException().getMessage()
                .contains("until-successful retries exhausted. Last exception message was: " + EXPECTED_FAILURE_MSG);
      }
    }), eq(mockEvent));
    verify(mockDLQ, never()).process(any(MuleEvent.class));
  }

  @Test
  public void alwaysFailMessageWrapUsingFailureExpression() throws Exception {
    when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(null);
    when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
    executeUntilSuccessfulFailingRoute(() -> {
      throw new MessagingException(mockEvent, new RuntimeException(EXPECTED_FAILURE_MSG));
    });
    waitUntilRouteIsExecuted();
    waitUntilExceptionIsHandled();

    verify(mockFlow.getExceptionListener(), times(1)).handleException(argThat(new ArgumentMatcher<MessagingException>() {

      @Override
      public boolean matches(Object item) {
        return item instanceof MessagingException
            && ((MessagingException) item).getCauseException() instanceof RetryPolicyExhaustedException
            && (((MessagingException) item).getCauseException()).getMessage()
                .contains("until-successful retries exhausted. Last exception message was: " + EXPECTED_FAILURE_MSG);
      }
    }), eq(mockEvent));
    verify(mockDLQ, never()).process(any(MuleEvent.class));
  }

  @Test
  public void alwaysFailUsingFailureExpressionDLQ() throws Exception {
    when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(mockDLQ);
    when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
    executeUntilSuccessfulFailingRoute(() -> {
      throw new RuntimeException(EXPECTED_FAILURE_MSG);
    });
    waitUntilRouteIsExecuted();
    waitUntilExceptionIsHandled();

    verify(mockFlow.getExceptionListener(), never()).handleException(any(MessagingException.class),
                                                                     any(MuleEvent.class));
    verify(mockDLQ, times(1)).process(argThat(new ArgumentMatcher<MuleEvent>() {

      @Override
      public boolean matches(Object argument) {
        MuleEvent argEvent = (MuleEvent) argument;

        assertThat(argEvent.getMessage().getExceptionPayload().getException().getMessage(),
                   containsString("until-successful retries exhausted. Last exception message was: " + EXPECTED_FAILURE_MSG));

        return true;
      }
    }));
  }

  @Test
  public void alwaysFailMessageUsingFailureExpressionDLQ() throws Exception {
    when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(mockDLQ);
    when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
    executeUntilSuccessfulFailingRoute(() -> {
      throw new MessagingException(CoreMessages.createStaticMessage(EXPECTED_FAILURE_MSG), mockEvent, mockRoute);
    });
    waitUntilRouteIsExecuted();
    waitUntilExceptionIsHandled();

    verify(mockFlow.getExceptionListener(), never()).handleException(any(MessagingException.class),
                                                                     any(MuleEvent.class));
    verify(mockDLQ, times(1)).process(argThat(new ArgumentMatcher<MuleEvent>() {

      @Override
      public boolean matches(Object argument) {
        MuleEvent argEvent = (MuleEvent) argument;
        assertThat(argEvent.getMessage().getPayload(), sameInstance(mockEvent.getMessage().getPayload()));
        assertThat(argEvent.getMessage().getExceptionPayload().getException().getMessage(),
                   containsString("until-successful retries exhausted. Last exception message was: " + EXPECTED_FAILURE_MSG));

        return true;
      }
    }));
  }

  @Test
  public void alwaysFailMessageWrapUsingFailureExpressionDLQ() throws Exception {
    when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(mockDLQ);
    when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
    executeUntilSuccessfulFailingRoute(() -> {
      throw new MessagingException(mockEvent, new RuntimeException(EXPECTED_FAILURE_MSG));
    });
    waitUntilRouteIsExecuted();
    waitUntilExceptionIsHandled();

    verify(mockFlow.getExceptionListener(), never()).handleException(any(MessagingException.class),
                                                                     any(MuleEvent.class));
    verify(mockDLQ, times(1)).process(argThat(new ArgumentMatcher<MuleEvent>() {

      @Override
      public boolean matches(Object argument) {
        MuleEvent argEvent = (MuleEvent) argument;

        assertThat(argEvent.getMessage().getExceptionPayload().getException().getMessage(),
                   containsString("until-successful retries exhausted. Last exception message was: " + EXPECTED_FAILURE_MSG));

        return true;
      }
    }));
  }

  @Test
  public void successfulExecution() throws Exception {
    executeUntilSuccessful();
    waitUntilRouteIsExecuted();
    verify(mockRoute, times(1)).process(mockEvent);
    verify(mockFlow.getExceptionListener(), never()).handleException(any(MessagingException.class), eq(mockEvent));
  }

  @Test
  public void successfulExecutionWithAckExpression() throws Exception {
    String ackExpression = "some-expression";
    String expressionEvalutaionResult = "new payload";
    when(mockUntilSuccessfulConfiguration.getAckExpression()).thenReturn(ackExpression);
    when(mockUntilSuccessfulConfiguration.getMuleContext().getExpressionManager().evaluate(ackExpression, mockEvent, null))
        .thenReturn(expressionEvalutaionResult);
    executeUntilSuccessful();
    waitUntilRouteIsExecuted();
    verify(mockRoute, times(1)).process(mockEvent);
    verify(mockUntilSuccessfulConfiguration.getMuleContext().getExpressionManager(), times(1)).evaluate(ackExpression, mockEvent,
                                                                                                        null);
    verify(mockEvent, times(1)).setMessage(argThat(new ArgumentMatcher<MuleMessage>() {

      @Override
      public boolean matches(Object argument) {
        assertThat(((MuleMessage) argument).getPayload(), equalTo(expressionEvalutaionResult));
        return true;
      }
    }));
    verify(mockFlow.getExceptionListener(), never()).handleException(any(MessagingException.class), eq(mockEvent));
  }

  private void executeUntilSuccessfulFailingRoute(FailCallback failCallback) throws Exception {
    failRoute = failCallback;
    routeCountDownLatch = new CountDownLatch(DEFAULT_TRIES);
    AsynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    processingStrategy.route(mockEvent, mock(FlowConstruct.class));
  }

  private void executeUntilSuccessful() throws Exception {
    routeCountDownLatch = new Latch();
    AsynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    processingStrategy.route(mockEvent, mock(FlowConstruct.class));
  }

  private void configureMockRouteToCountDownRouteLatch() throws MuleException {
    when(mockRoute.process(any(MuleEvent.class))).thenAnswer(invocationOnMock -> {
      routeCountDownLatch.countDown();
      failRoute.doFail();
      return invocationOnMock.getArguments()[0];
    });
  }

  private void configureMockPoolToInvokeRunnableInNewThread() {
    doAnswer(invocationOnMock -> {
      new Thread(() -> ((Runnable) invocationOnMock.getArguments()[0]).run()).start();
      return null;
    }).when(mockPool).execute(any(Runnable.class));
  }

  private void configureMockScheduledPoolToInvokeRunnableInNewThread() {
    when(mockScheduledPool.schedule(any(Callable.class), anyLong(), any(TimeUnit.class))).thenAnswer(invocationOnMock -> {
      assertThat((Long) invocationOnMock.getArguments()[1], is(mockUntilSuccessfulConfiguration.getMillisBetweenRetries()));
      assertThat((TimeUnit) invocationOnMock.getArguments()[2], is(MILLISECONDS));
      new Thread(() -> {
        try {
          ((Callable) invocationOnMock.getArguments()[0]).call();
        } catch (Exception e) {
          // Do nothing.
        }
      }).start();
      return null;
    });
  }

  private void waitUntilRouteIsExecuted() throws InterruptedException {
    if (!routeCountDownLatch.await(200000, MILLISECONDS)) {
      fail("route should be executed " + routeCountDownLatch.getCount() + " times");
    }
  }

  private AsynchronousUntilSuccessfulProcessingStrategy createProcessingStrategy() throws Exception {
    AsynchronousUntilSuccessfulProcessingStrategy processingStrategy = new AsynchronousUntilSuccessfulProcessingStrategy();
    processingStrategy.setUntilSuccessfulConfiguration(mockUntilSuccessfulConfiguration);
    processingStrategy.setMessagingExceptionHandler(mockFlow.getExceptionListener());
    processingStrategy.setMuleContext(muleContext);
    processingStrategy.initialise();
    processingStrategy.start();
    return processingStrategy;
  }

  private void waitUntilExceptionIsHandled() throws InterruptedException {
    if (!exceptionHandlingLatch.await(100000, MILLISECONDS)) {
      fail("exception should be handled");
    }
  }

  private void configureExceptionStrategyToReleaseLatchWhenExecuted() {
    when(mockFlow.getExceptionListener().handleException(any(MessagingException.class), any(MuleEvent.class)))
        .thenAnswer(invocationOnMock -> {
          exceptionHandlingLatch.release();
          return null;
        });
  }

  private void configureDLQToReleaseLatchWhenExecuted() throws MuleException {
    when(mockDLQ.process(any(MuleEvent.class))).thenAnswer(invocationOnMock -> {
      exceptionHandlingLatch.release();
      return null;
    });
  }

}
