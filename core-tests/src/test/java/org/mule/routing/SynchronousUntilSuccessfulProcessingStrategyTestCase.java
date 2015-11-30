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
import static org.mockito.Mockito.spy;
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
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class SynchronousUntilSuccessfulProcessingStrategyTestCase extends AbstractMuleContextTestCase
{

    private static final int DEFAULT_RETRIES = 4;
    private static final String TEST_DATA = "Test Data";
    private static final String PROCESSED_DATA = "Processed Data";
    private UntilSuccessfulConfiguration mockUntilSuccessfulConfiguration = mock(UntilSuccessfulConfiguration.class, Answers.RETURNS_DEEP_STUBS.get());
    private MuleEvent event;
    private MessageProcessor mockRoute = mock(MessageProcessor.class, Answers.RETURNS_DEEP_STUBS.get());
    private ExpressionFilter mockAlwaysTrueFailureExpressionFilter = mock(ExpressionFilter.class, Answers.RETURNS_DEEP_STUBS.get());
    private ThreadingProfile mockThreadingProfile = mock(ThreadingProfile.class, Answers.RETURNS_DEEP_STUBS.get());
    private ListableObjectStore<MuleEvent> mockObjectStore = mock(ListableObjectStore.class, Answers.RETURNS_DEEP_STUBS.get());

    @Before
    public void setUp() throws Exception
    {
        when(mockAlwaysTrueFailureExpressionFilter.accept(any(MuleMessage.class))).thenReturn(true);
        when(mockUntilSuccessfulConfiguration.getRoute()).thenReturn(mockRoute);
        when(mockUntilSuccessfulConfiguration.getAckExpression()).thenReturn(null);
        when(mockUntilSuccessfulConfiguration.getMaxRetries()).thenReturn(DEFAULT_RETRIES);
        when(mockUntilSuccessfulConfiguration.getThreadingProfile()).thenReturn(null);
        when(mockUntilSuccessfulConfiguration.getObjectStore()).thenReturn(null);
        when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(null);
        event = getTestEvent(TEST_DATA);
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
            processingStrategy.route(event);
            fail("processing should throw exception");
        }
        catch (MessagingException e)
        {
            assertThat(e, instanceOf(RoutingException.class));
            verify(mockRoute, times(DEFAULT_RETRIES + 1)).process(event);
        }
    }

    @Test
    public void alwaysFailUsingFailureExpression() throws MuleException
    {
        when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
        SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
        try
        {
            processingStrategy.route(event);
            fail("processing should throw exception");
        }
        catch (MessagingException e)
        {
            assertThat(e, instanceOf(RoutingException.class));
            verify(mockRoute, times(DEFAULT_RETRIES + 1)).process(event);
            verify(mockAlwaysTrueFailureExpressionFilter, times(DEFAULT_RETRIES + 1)).accept(any(MuleMessage.class));
        }
    }

    @Test
    public void successfulExecution() throws Exception
    {
        SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
        when(mockRoute.process(event)).thenReturn(event);
        assertThat(processingStrategy.route(event), is(event));
        verify(mockRoute).process(event);
    }

    @Test
    public void retryOnOriginalEvent() throws Exception
    {
        when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
        SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
        when(mockRoute.process(any(MuleEvent.class))).then(new Answer<MuleEvent>()
        {
            @Override
            public MuleEvent answer(InvocationOnMock invocation) throws Throwable
            {
                MuleEvent argEvent = (MuleEvent) invocation.getArguments()[0];
                assertThat(argEvent.getMessageAsString(), is(TEST_DATA));
                argEvent.getMessage().setPayload(PROCESSED_DATA);
                return argEvent;
            }
        });
        try
        {
            processingStrategy.route(event);
            fail("processing should throw exception");
        }
        catch (MessagingException e)
        {
            assertThat(e, instanceOf(RoutingException.class));
            verify(mockRoute, times(DEFAULT_RETRIES + 1)).process(event);
            verify(mockAlwaysTrueFailureExpressionFilter, times(DEFAULT_RETRIES + 1)).accept(any(MuleMessage.class));
        }
    }

    @Test
    public void successfulExecutionWithAckExpression() throws Exception
    {
        String ackExpression = "some-expression";
        String expressionEvalutaionResult = "new payload";
        event.setMessage(spy(event.getMessage()));
        when(mockUntilSuccessfulConfiguration.getAckExpression()).thenReturn(ackExpression);
        when(mockUntilSuccessfulConfiguration.getMuleContext().getExpressionManager().evaluate(ackExpression, event)).thenReturn(expressionEvalutaionResult);
        SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
        when(mockRoute.process(event)).thenReturn(event);
        assertThat(processingStrategy.route(event), is(event));
        verify(mockRoute).process(event);
        verify(mockUntilSuccessfulConfiguration.getMuleContext().getExpressionManager()).evaluate(ackExpression, event);
        verify(event.getMessage()).setPayload(expressionEvalutaionResult);
    }

    @Test
    public void successfulWithNullResponseFromRoute() throws Exception
    {
        when(mockRoute.process(event)).thenReturn(null);
        SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
        assertThat((VoidMuleEvent) processingStrategy.route(event), is(VoidMuleEvent.getInstance()));
    }

    @Test
    public void successfulWithNullEventResponseFromRoute() throws Exception
    {
        when(mockRoute.process(event)).thenReturn(VoidMuleEvent.getInstance());
        SynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
        assertThat((VoidMuleEvent) processingStrategy.route(event), is(VoidMuleEvent.getInstance()));
    }

    private SynchronousUntilSuccessfulProcessingStrategy createProcessingStrategy() throws InitialisationException
    {
        SynchronousUntilSuccessfulProcessingStrategy processingStrategy = new SynchronousUntilSuccessfulProcessingStrategy();
        processingStrategy.setUntilSuccessfulConfiguration(mockUntilSuccessfulConfiguration);
        processingStrategy.initialise();
        return processingStrategy;
    }

}
