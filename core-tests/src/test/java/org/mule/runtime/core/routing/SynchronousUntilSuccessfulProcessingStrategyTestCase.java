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
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.api.Event.getCurrentEvent;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.store.ListableObjectStore;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

public class SynchronousUntilSuccessfulProcessingStrategyTestCase extends AbstractMuleContextTestCase {

  private static final int DEFAULT_RETRIES = 4;
  private static final String PROCESSED_DATA = "Processed Data";
  private UntilSuccessfulConfiguration mockUntilSuccessfulConfiguration =
      mock(UntilSuccessfulConfiguration.class, Answers.RETURNS_DEEP_STUBS.get());
  private Processor mockRoute = mock(Processor.class, Answers.RETURNS_DEEP_STUBS.get());
  private ListableObjectStore<Event> mockObjectStore = mock(ListableObjectStore.class, Answers.RETURNS_DEEP_STUBS.get());
  private Flow flow;

  @Before
  public void setUp() throws Exception {
    when(mockUntilSuccessfulConfiguration.getRoute()).thenReturn(mockRoute);
    when(mockUntilSuccessfulConfiguration.getAckExpression()).thenReturn(null);
    when(mockUntilSuccessfulConfiguration.getMaxRetries()).thenReturn(DEFAULT_RETRIES);
    when(mockUntilSuccessfulConfiguration.getObjectStore()).thenReturn(null);
    when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(null);

    flow = getTestFlow(muleContext);
    when(mockUntilSuccessfulConfiguration.getFlowConstruct()).thenReturn(flow);

    setCurrentEvent(null);
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
      processingStrategy.route(testEvent(), flow);
      fail("processing should throw exception");
    } catch (RoutingException e) {
      verify(mockRoute, times(DEFAULT_RETRIES + 1)).process(any(Event.class));
    }
  }

  @Test
  public void alwaysFailUsingFailureExpression() throws Exception {
    final Event testEvent = testEvent();
    when(mockRoute.process(any(Event.class))).thenReturn(testEvent);
    when(mockUntilSuccessfulConfiguration.getFailureExpression()).thenReturn("true");
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    try {
      processingStrategy.route(testEvent(), flow);
      fail("processing should throw exception");
    } catch (RoutingException e) {
      verify(mockRoute, times(DEFAULT_RETRIES + 1)).process(any(Event.class));
    }
  }

  @Test
  public void successfulExecution() throws Exception {
    when(mockUntilSuccessfulConfiguration.getMuleContext().getExpressionManager()
        .evaluateBoolean(anyString(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(true);

    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    when(mockRoute.process(any(Event.class))).thenAnswer(invocation -> (Event) invocation.getArguments()[0]);
    when(mockUntilSuccessfulConfiguration.getFailureExpression()).thenReturn("false");
    Event response = processingStrategy.route(testEvent(), flow);
    assertThat(response.getMessage().getPayload().getValue(), equalTo(testEvent().getMessage().getPayload().getValue()));
    verify(mockRoute).process(any(Event.class));
    assertThat(getCurrentEvent(), sameInstance(response));
  }

  @Test
  public void retryOnOriginalEvent() throws Exception {
    when(mockUntilSuccessfulConfiguration.getFailureExpression()).thenReturn("true");
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    when(mockRoute.process(any(Event.class))).then(invocation -> {
      Event argEvent = (Event) invocation.getArguments()[0];
      assertThat(argEvent.getMessageAsString(muleContext), is(TEST_PAYLOAD));
      return Event.builder(argEvent).message(InternalMessage.builder(argEvent.getMessage()).payload(PROCESSED_DATA).build())
          .build();
    });
    try {
      processingStrategy.route(testEvent(), flow);
      fail("processing should throw exception");
    } catch (RoutingException e) {
      assertThat(e, instanceOf(RoutingException.class));
      verify(mockRoute, times(DEFAULT_RETRIES + 1)).process(any(Event.class));
    }
  }

  @Test
  public void successfulExecutionWithAckExpression() throws Exception {
    String ackExpression = "some-expression";
    String expressionEvaluationResult = "new payload";
    when(mockUntilSuccessfulConfiguration.getAckExpression()).thenReturn(ackExpression);
    TypedValue<String> typedValue = new TypedValue<>(expressionEvaluationResult, STRING);
    when(mockUntilSuccessfulConfiguration.getMuleContext().getExpressionManager()
        .evaluate(eq(ackExpression), any(Event.class))).thenReturn(typedValue);
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    when(mockRoute.process(any(Event.class))).thenAnswer(invocation -> (Event) invocation.getArguments()[0]);
    when(mockUntilSuccessfulConfiguration.getFailureExpression()).thenReturn("false");
    Event response = processingStrategy.route(testEvent(), flow);
    assertThat(response.getMessage().getPayload().getValue(), equalTo(expressionEvaluationResult));
    verify(mockRoute).process(any(Event.class));
    verify(mockUntilSuccessfulConfiguration.getMuleContext().getExpressionManager()).evaluate(eq(ackExpression),
                                                                                              any(Event.class));
  }

  @Test
  public void successfulWithNullResponseFromRoute() throws Exception {
    when(mockRoute.process(any(Event.class))).thenReturn(null);
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    assertThat(processingStrategy.route(testEvent(), flow), is(nullValue()));
  }

  private SynchronousUntilSuccessfulProcessingStrategy createProcessingStrategy() throws InitialisationException {
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = new SynchronousUntilSuccessfulProcessingStrategy();
    processingStrategy.setMuleContext(muleContext);
    processingStrategy.setUntilSuccessfulConfiguration(mockUntilSuccessfulConfiguration);
    processingStrategy.initialise();
    return processingStrategy;
  }

}
