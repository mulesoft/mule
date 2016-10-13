/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.mule.runtime.core.api.Event.getCurrentEvent;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.store.ListableObjectStore;
import org.mule.runtime.core.routing.filters.ExpressionFilter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

public class SynchronousUntilSuccessfulProcessingStrategyTestCase extends AbstractMuleContextTestCase {

  private static final int DEFAULT_RETRIES = 4;
  private static final String PROCESSED_DATA = "Processed Data";
  private UntilSuccessfulConfiguration mockUntilSuccessfulConfiguration =
      mock(UntilSuccessfulConfiguration.class, Answers.RETURNS_DEEP_STUBS.get());
  private Processor mockRoute = mock(Processor.class, Answers.RETURNS_DEEP_STUBS.get());
  private ExpressionFilter mockAlwaysTrueFailureExpressionFilter = mock(ExpressionFilter.class, Answers.RETURNS_DEEP_STUBS.get());
  private ThreadingProfile mockThreadingProfile = mock(ThreadingProfile.class, Answers.RETURNS_DEEP_STUBS.get());
  private ListableObjectStore<Event> mockObjectStore = mock(ListableObjectStore.class, Answers.RETURNS_DEEP_STUBS.get());

  @Before
  public void setUp() throws Exception {
    when(mockAlwaysTrueFailureExpressionFilter.accept(any(Event.class), any(Event.Builder.class))).thenReturn(true);
    when(mockUntilSuccessfulConfiguration.getRoute()).thenReturn(mockRoute);
    when(mockUntilSuccessfulConfiguration.getAckExpression()).thenReturn(null);
    when(mockUntilSuccessfulConfiguration.getMaxRetries()).thenReturn(DEFAULT_RETRIES);
    when(mockUntilSuccessfulConfiguration.getThreadingProfile()).thenReturn(null);
    when(mockUntilSuccessfulConfiguration.getObjectStore()).thenReturn(null);
    when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(null);
    setCurrentEvent(null);
  }

  @Test(expected = InitialisationException.class)
  public void failWhenThreadingProfileIsConfigured() throws Exception {
    when(mockUntilSuccessfulConfiguration.getThreadingProfile()).thenReturn(mockThreadingProfile);
    createProcessingStrategy();
  }

  @Test(expected = InitialisationException.class)
  public void failWhenObjectStoreIsConfigured() throws Exception {
    when(mockUntilSuccessfulConfiguration.getObjectStore()).thenReturn(mockObjectStore);
    createProcessingStrategy();
  }

  @Test(expected = InitialisationException.class)
  public void failWhenDlqIsConfigured() throws Exception {
    when(mockUntilSuccessfulConfiguration.getObjectStore()).thenReturn(mockObjectStore);
    when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(mockRoute);
    createProcessingStrategy();
  }

  @Test
  public void alwaysFail() throws Exception {
    when(mockRoute.process(any(Event.class))).thenThrow(new RuntimeException("expected failure"));
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    try {
      processingStrategy.route(testEvent(), getTestFlow(muleContext));
      fail("processing should throw exception");
    } catch (RoutingException e) {
      verify(mockRoute, times(DEFAULT_RETRIES + 1)).process(any(Event.class));
    }
  }

  @Test
  public void alwaysFailUsingFailureExpression() throws Exception {
    final Event testEvent = testEvent();
    when(mockRoute.process(any(Event.class))).thenReturn(testEvent);
    when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    try {
      processingStrategy.route(testEvent(), getTestFlow(muleContext));
      fail("processing should throw exception");
    } catch (RoutingException e) {
      verify(mockRoute, times(DEFAULT_RETRIES + 1)).process(any(Event.class));
      verify(mockAlwaysTrueFailureExpressionFilter, times(DEFAULT_RETRIES + 1)).accept(any(Event.class),
                                                                                       any(Event.Builder.class));
    }
  }

  @Test
  public void successfulExecution() throws Exception {
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    when(mockRoute.process(any(Event.class))).thenAnswer(invocation -> (Event) invocation.getArguments()[0]);
    Event response = processingStrategy.route(testEvent(), getTestFlow(muleContext));
    assertThat(response.getMessage().getPayload().getValue(), equalTo(testEvent().getMessage().getPayload().getValue()));
    verify(mockRoute).process(any(Event.class));
    assertThat(getCurrentEvent(), sameInstance(response));
  }

  @Test
  public void retryOnOriginalEvent() throws Exception {
    when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    when(mockRoute.process(any(Event.class))).then(invocation -> {
      Event argEvent = (Event) invocation.getArguments()[0];
      assertThat(argEvent.getMessageAsString(muleContext), is(TEST_PAYLOAD));
      return Event.builder(argEvent).message(InternalMessage.builder(argEvent.getMessage()).payload(PROCESSED_DATA).build())
          .build();
    });
    try {
      processingStrategy.route(testEvent(), getTestFlow(muleContext));
      fail("processing should throw exception");
    } catch (RoutingException e) {
      assertThat(e, instanceOf(RoutingException.class));
      verify(mockRoute, times(DEFAULT_RETRIES + 1)).process(any(Event.class));
      verify(mockAlwaysTrueFailureExpressionFilter, times(DEFAULT_RETRIES + 1)).accept(any(Event.class),
                                                                                       any(Event.Builder.class));
    }
  }

  @Test
  public void successfulExecutionWithAckExpression() throws Exception {
    String ackExpression = "some-expression";
    String expressionEvalutaionResult = "new payload";
    when(mockUntilSuccessfulConfiguration.getAckExpression()).thenReturn(ackExpression);
    when(mockUntilSuccessfulConfiguration.getMuleContext().getExpressionLanguage()
        .evaluate(eq(ackExpression), any(Event.class), eq(null))).thenReturn(expressionEvalutaionResult);
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    when(mockRoute.process(any(Event.class))).thenAnswer(invocation -> (Event) invocation.getArguments()[0]);
    Event response = processingStrategy.route(testEvent(), getTestFlow(muleContext));
    assertThat(response.getMessage().getPayload().getValue(), equalTo(expressionEvalutaionResult));
    verify(mockRoute).process(any(Event.class));
    verify(mockUntilSuccessfulConfiguration.getMuleContext().getExpressionLanguage()).evaluate(eq(ackExpression),
                                                                                               any(Event.class), eq(null));
  }

  @Test
  public void successfulWithNullResponseFromRoute() throws Exception {
    when(mockRoute.process(any(Event.class))).thenReturn(null);
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    assertThat(processingStrategy.route(testEvent(), getTestFlow(muleContext)), is(nullValue()));
  }

  @Test
  public void successfulWithNullEventResponseFromRoute() throws Exception {
    when(mockRoute.process(any(Event.class))).thenReturn(null);
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    assertThat(processingStrategy.route(testEvent(), getTestFlow(muleContext)), is(nullValue()));
  }

  private SynchronousUntilSuccessfulProcessingStrategy createProcessingStrategy() throws InitialisationException {
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = new SynchronousUntilSuccessfulProcessingStrategy();
    processingStrategy.setUntilSuccessfulConfiguration(mockUntilSuccessfulConfiguration);
    processingStrategy.initialise();
    return processingStrategy;
  }

}
