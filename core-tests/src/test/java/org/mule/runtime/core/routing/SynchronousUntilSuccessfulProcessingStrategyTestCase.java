/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.mule.runtime.core.DefaultMuleEvent.getCurrentEvent;
import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.store.ListableObjectStore;
import org.mule.runtime.core.routing.filters.ExpressionFilter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

public class SynchronousUntilSuccessfulProcessingStrategyTestCase extends AbstractMuleContextTestCase {

  private static final int DEFAULT_RETRIES = 4;
  private static final String TEST_DATA = "Test Data";
  private static final String PROCESSED_DATA = "Processed Data";
  private UntilSuccessfulConfiguration mockUntilSuccessfulConfiguration =
      mock(UntilSuccessfulConfiguration.class, Answers.RETURNS_DEEP_STUBS.get());
  private MuleEvent event;
  private MessageProcessor mockRoute = mock(MessageProcessor.class, Answers.RETURNS_DEEP_STUBS.get());
  private ExpressionFilter mockAlwaysTrueFailureExpressionFilter = mock(ExpressionFilter.class, Answers.RETURNS_DEEP_STUBS.get());
  private ThreadingProfile mockThreadingProfile = mock(ThreadingProfile.class, Answers.RETURNS_DEEP_STUBS.get());
  private ListableObjectStore<MuleEvent> mockObjectStore = mock(ListableObjectStore.class, Answers.RETURNS_DEEP_STUBS.get());

  @Before
  public void setUp() throws Exception {
    when(mockAlwaysTrueFailureExpressionFilter.accept(any(MuleEvent.class))).thenReturn(true);
    when(mockUntilSuccessfulConfiguration.getRoute()).thenReturn(mockRoute);
    when(mockUntilSuccessfulConfiguration.getAckExpression()).thenReturn(null);
    when(mockUntilSuccessfulConfiguration.getMaxRetries()).thenReturn(DEFAULT_RETRIES);
    when(mockUntilSuccessfulConfiguration.getThreadingProfile()).thenReturn(null);
    when(mockUntilSuccessfulConfiguration.getObjectStore()).thenReturn(null);
    when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(null);
    event = getTestEvent(TEST_DATA);
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
    when(mockRoute.process(any(MuleEvent.class))).thenThrow(new RuntimeException("expected failure"));
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    try {
      processingStrategy.route(event, getTestFlow());
      fail("processing should throw exception");
    } catch (MessagingException e) {
      assertThat(e, instanceOf(RoutingException.class));
      verify(mockRoute, times(DEFAULT_RETRIES + 1)).process(event);
      assertThat(getCurrentEvent(), sameInstance(e.getEvent()));
    }
  }

  @Test
  public void alwaysFailUsingFailureExpression() throws Exception {
    when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    try {
      processingStrategy.route(event, getTestFlow());
      fail("processing should throw exception");
    } catch (MessagingException e) {
      assertThat(e, instanceOf(RoutingException.class));
      verify(mockRoute, times(DEFAULT_RETRIES + 1)).process(event);
      verify(mockAlwaysTrueFailureExpressionFilter, times(DEFAULT_RETRIES + 1)).accept(any(MuleEvent.class));
    }
  }

  @Test
  public void successfulExecution() throws Exception {
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    when(mockRoute.process(event)).thenAnswer(invocation -> (MuleEvent) invocation.getArguments()[0]);
    MuleEvent response = processingStrategy.route(event, getTestFlow());
    assertThat(response.getMessage(), is(event.getMessage()));
    verify(mockRoute).process(event);
    assertThat(getCurrentEvent(), sameInstance(response));
  }

  @Test
  public void retryOnOriginalEvent() throws Exception {
    when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    when(mockRoute.process(any(MuleEvent.class))).then(invocation -> {
      MuleEvent argEvent = (MuleEvent) invocation.getArguments()[0];
      assertThat(argEvent.getMessageAsString(muleContext), is(TEST_DATA));
      argEvent.setMessage(MuleMessage.builder(argEvent.getMessage()).payload(PROCESSED_DATA).build());
      return argEvent;
    });
    try {
      processingStrategy.route(event, getTestFlow());
      fail("processing should throw exception");
    } catch (MessagingException e) {
      assertThat(e, instanceOf(RoutingException.class));
      verify(mockRoute, times(DEFAULT_RETRIES + 1)).process(event);
      verify(mockAlwaysTrueFailureExpressionFilter, times(DEFAULT_RETRIES + 1)).accept(any(MuleEvent.class));
    }
  }

  @Test
  public void successfulExecutionWithAckExpression() throws Exception {
    String ackExpression = "some-expression";
    String expressionEvalutaionResult = "new payload";
    when(mockUntilSuccessfulConfiguration.getAckExpression()).thenReturn(ackExpression);
    when(mockUntilSuccessfulConfiguration.getMuleContext().getExpressionManager().evaluate(ackExpression, event, null))
        .thenReturn(expressionEvalutaionResult);
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    when(mockRoute.process(any(MuleEvent.class))).thenAnswer(invocation -> (MuleEvent) invocation.getArguments()[0]);
    MuleEvent response = processingStrategy.route(event, getTestFlow());
    assertThat(response.getMessage().getPayload(), equalTo(expressionEvalutaionResult));
    verify(mockRoute).process(any(MuleEvent.class));
    verify(mockUntilSuccessfulConfiguration.getMuleContext().getExpressionManager()).evaluate(ackExpression, event, null);
  }

  @Test
  public void successfulWithNullResponseFromRoute() throws Exception {
    when(mockRoute.process(event)).thenReturn(null);
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    assertThat(processingStrategy.route(event, getTestFlow()), is(event));
  }

  @Test
  public void successfulWithNullEventResponseFromRoute() throws Exception {
    when(mockRoute.process(event)).thenReturn(VoidMuleEvent.getInstance());
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
    assertThat(processingStrategy.route(event, getTestFlow()), is(event));
  }

  private SynchronousUntilSuccessfulProcessingStrategy createProcessingStrategy() throws InitialisationException {
    SynchronousUntilSuccessfulProcessingStrategy processingStrategy = new SynchronousUntilSuccessfulProcessingStrategy();
    processingStrategy.setUntilSuccessfulConfiguration(mockUntilSuccessfulConfiguration);
    processingStrategy.initialise();
    return processingStrategy;
  }

}
