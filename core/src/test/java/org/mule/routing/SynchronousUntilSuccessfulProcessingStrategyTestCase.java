/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.RoutingException;
import org.mule.api.store.ListableObjectStore;
import org.mule.routing.filters.ExpressionFilter;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

@SmallTest
public class SynchronousUntilSuccessfulProcessingStrategyTestCase extends AbstractMuleTestCase
{

    private static final int DEFAULT_RETRIES = 4;
    private UntilSuccessfulConfiguration mockUntilSuccessfulConfiguration = mock(UntilSuccessfulConfiguration.class, Answers.RETURNS_DEEP_STUBS.get());
    private MuleEvent mockEvent = mock(MuleEvent.class, Answers.RETURNS_DEEP_STUBS.get());
    private MessageProcessor mockRoute = mock(MessageProcessor.class, Answers.RETURNS_DEEP_STUBS.get());
    private ExpressionFilter mockAlwaysTrueFailureExpressionFilter = mock(ExpressionFilter.class, Answers.RETURNS_DEEP_STUBS.get());
    private ThreadingProfile mockThreadingProfile = mock(ThreadingProfile.class, Answers.RETURNS_DEEP_STUBS.get());
    private ListableObjectStore<MuleEvent> mockObjectStore = mock(ListableObjectStore.class, Answers.RETURNS_DEEP_STUBS.get());

    @Before
    public void setUp()
    {
        when(mockAlwaysTrueFailureExpressionFilter.accept(any(MuleMessage.class))).thenReturn(true);
        when(mockUntilSuccessfulConfiguration.getRoute()).thenReturn(mockRoute);
        when(mockUntilSuccessfulConfiguration.getAckExpression()).thenReturn(null);
        when(mockUntilSuccessfulConfiguration.getMaxRetries()).thenReturn(DEFAULT_RETRIES);
        when(mockUntilSuccessfulConfiguration.getThreadingProfile()).thenReturn(null);
        when(mockUntilSuccessfulConfiguration.getObjectStore()).thenReturn(null);
        when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(null);
    }

    @Test(expected = InitialisationException.class)
    public void failWhenThreadingProfileIsConfigured() throws Exception
    {
        when(mockUntilSuccessfulConfiguration.getThreadingProfile()).thenReturn(mockThreadingProfile);
        createProcessingStrategy();
    }

    @Test(expected = InitialisationException.class)
    public void failWhenObjectStoreIsConfigured() throws Exception
    {
        when(mockUntilSuccessfulConfiguration.getObjectStore()).thenReturn(mockObjectStore);
        createProcessingStrategy();
    }

    @Test(expected = InitialisationException.class)
    public void failWhenDlqIsConfigured() throws Exception
    {
        when(mockUntilSuccessfulConfiguration.getObjectStore()).thenReturn(mockObjectStore);
        when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(mockRoute);
        createProcessingStrategy();
    }

    @Test
    public void alwaysFail() throws MuleException
    {
        when(mockRoute.process(any(MuleEvent.class))).thenThrow(new RuntimeException("expected failure"));
        SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
        try
        {
            processingStrategy.route(mockEvent);
            fail("processing should thrown exception");
        }
        catch (MessagingException e)
        {
            assertThat(e, instanceOf(RoutingException.class));
            verify(mockRoute, times(DEFAULT_RETRIES + 1)).process(mockEvent);
        }
    }

    @Test
    public void alwaysFailUsingFailureExpression() throws MuleException
    {
        when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
        SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
        try
        {
            processingStrategy.route(mockEvent);
            fail("processing should thrown exception");
        }
        catch (MessagingException e)
        {
            assertThat(e, instanceOf(RoutingException.class));
            verify(mockRoute, times(DEFAULT_RETRIES + 1)).process(mockEvent);
            verify(mockAlwaysTrueFailureExpressionFilter, times(DEFAULT_RETRIES + 1)).accept(any(MuleMessage.class));
        }
    }

    @Test
    public void successfulExecution() throws Exception
    {
        SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
        when(mockRoute.process(mockEvent)).thenReturn(mockEvent);
        assertThat(processingStrategy.route(mockEvent), is(mockEvent));
        verify(mockRoute, times(1)).process(mockEvent);
    }

    @Test
    public void successfulExecutionWithAckExpression() throws Exception
    {
        String ackExpression = "some-expression";
        String expressionEvalutaionResult = "new payload";
        when(mockUntilSuccessfulConfiguration.getAckExpression()).thenReturn(ackExpression);
        when(mockUntilSuccessfulConfiguration.getMuleContext().getExpressionManager().evaluate(ackExpression, mockEvent)).thenReturn(expressionEvalutaionResult);
        SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
        when(mockRoute.process(mockEvent)).thenReturn(mockEvent);
        assertThat(processingStrategy.route(mockEvent), is(mockEvent));
        verify(mockRoute, times(1)).process(mockEvent);
        verify(mockUntilSuccessfulConfiguration.getMuleContext().getExpressionManager(), times(1)).evaluate(ackExpression, mockEvent);
        verify(mockEvent.getMessage(), times(1)).setPayload(expressionEvalutaionResult);
    }

    @Test
    public void successfulWithNullResponseFromRoute() throws Exception
    {
        when(mockRoute.process(mockEvent)).thenReturn(null);
        SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
        assertThat((VoidMuleEvent) processingStrategy.route(mockEvent), is(VoidMuleEvent.getInstance()));
    }

    @Test
    public void successfulWithNullEventResponseFromRoute() throws Exception
    {
        when(mockRoute.process(mockEvent)).thenReturn(VoidMuleEvent.getInstance());
        SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
        assertThat((VoidMuleEvent) processingStrategy.route(mockEvent), is(VoidMuleEvent.getInstance()));
    }

    private SynchronousUntilSuccessfulProcessingStrategy createProcessingStrategy() throws InitialisationException
    {
        SynchronousUntilSuccessfulProcessingStrategy processingStrategy = new SynchronousUntilSuccessfulProcessingStrategy();
        processingStrategy.setUntilSuccessfulConfiguration(mockUntilSuccessfulConfiguration);
        processingStrategy.initialise();
        return processingStrategy;
    }

}
