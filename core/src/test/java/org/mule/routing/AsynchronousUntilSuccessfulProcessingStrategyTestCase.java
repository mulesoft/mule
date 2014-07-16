/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.routing.UntilSuccessful.DEFAULT_PROCESS_ATTEMPT_COUNT_PROPERTY_VALUE;
import static org.mule.routing.UntilSuccessful.PROCESS_ATTEMPT_COUNT_PROPERTY_NAME;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.routing.filters.ExpressionFilter;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.concurrent.Latch;
import org.mule.util.store.SimpleMemoryObjectStore;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
public class AsynchronousUntilSuccessfulProcessingStrategyTestCase extends AbstractMuleTestCase
{

    private static final int DEFAULT_RETRIES = 4;
    private static final int DEFAULT_TRIES = DEFAULT_RETRIES + 1;

    private final Latch exceptionStrategyLatch = new Latch();
    private UntilSuccessfulConfiguration mockUntilSuccessfulConfiguration = mock(UntilSuccessfulConfiguration.class, Answers.RETURNS_DEEP_STUBS.get());
    private MuleEvent mockEvent = mock(MuleEvent.class, Answers.RETURNS_DEEP_STUBS.get());
    private MessageProcessor mockRoute = mock(MessageProcessor.class, Answers.RETURNS_DEEP_STUBS.get());
    private ExpressionFilter mockAlwaysTrueFailureExpressionFilter = mock(ExpressionFilter.class, Answers.RETURNS_DEEP_STUBS.get());
    private ScheduledThreadPoolExecutor mockScheduledPool = mock(ScheduledThreadPoolExecutor.class, Answers.RETURNS_DEEP_STUBS.get());
    private SimpleMemoryObjectStore<MuleEvent> objectStore = new SimpleMemoryObjectStore<MuleEvent>();
    private boolean failRoute;
    private CountDownLatch routeCountDownLatch;

    @Before
    public void setUp() throws Exception
    {
        when(mockAlwaysTrueFailureExpressionFilter.accept(any(MuleMessage.class))).thenReturn(true);
        when(mockUntilSuccessfulConfiguration.getRoute()).thenReturn(mockRoute);
        when(mockUntilSuccessfulConfiguration.getAckExpression()).thenReturn(null);
        when(mockUntilSuccessfulConfiguration.getMaxRetries()).thenReturn(DEFAULT_RETRIES);
        when(mockEvent.getMessage().getInvocationProperty(PROCESS_ATTEMPT_COUNT_PROPERTY_NAME, DEFAULT_PROCESS_ATTEMPT_COUNT_PROPERTY_VALUE)).thenAnswer(new Answer<Object>()
        {
            private int numberOfAttempts = 0;

            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                return numberOfAttempts++;
            }
        });
        when(mockUntilSuccessfulConfiguration.getThreadingProfile().createScheduledPool(anyString())).thenReturn(mockScheduledPool);
        when(mockUntilSuccessfulConfiguration.getObjectStore()).thenReturn(objectStore);
        objectStore.clear();
        configureMockScheduledPoolToInvokeRunnableInNewThread();
        configureMockRouteToCountDownRouteLatch();
        configureExceptionStrategyToReleaseLatchWhenExecuted();
    }

    @Test(expected = InitialisationException.class)
    public void failWhenObjectStoreIsNull() throws Exception
    {
        when(mockUntilSuccessfulConfiguration.getObjectStore()).thenReturn(null);
        createProcessingStrategy();
    }

    @Test
    public void alwaysFail() throws Exception
    {
        executeUntilSuccessfulFailingRoute();
        waitUntilRouteIsExecuted();
    }

    @Test
    public void alwaysFailUsingFailureExpression() throws Exception
    {
        when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(null);
        when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
        executeUntilSuccessfulFailingRoute();
        waitUntilRouteIsExecuted();
        waitUntilExceptionStrategyIsExecuted();
    }

    @Test
    public void successfulExecution() throws Exception
    {
        executeUntilSuccessful();
        waitUntilRouteIsExecuted();
        verify(mockRoute, times(1)).process(mockEvent);
    }

    @Test
    public void successfulExecutionWithAckExpression() throws Exception
    {
        String ackExpression = "some-expression";
        String expressionEvalutaionResult = "new payload";
        when(mockUntilSuccessfulConfiguration.getAckExpression()).thenReturn(ackExpression);
        when(mockUntilSuccessfulConfiguration.getMuleContext().getExpressionManager().evaluate(ackExpression, mockEvent)).thenReturn(expressionEvalutaionResult);
        executeUntilSuccessful();
        waitUntilRouteIsExecuted();
        verify(mockRoute, times(1)).process(mockEvent);
        verify(mockUntilSuccessfulConfiguration.getMuleContext().getExpressionManager(), times(1)).evaluate(ackExpression, mockEvent);
        verify(mockEvent.getMessage(), times(1)).setPayload(expressionEvalutaionResult);
    }

    private void executeUntilSuccessfulFailingRoute() throws Exception
    {
        failRoute = true;
        routeCountDownLatch = new CountDownLatch(DEFAULT_TRIES);
        AsynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
        processingStrategy.route(mockEvent);
    }

    private void executeUntilSuccessful() throws Exception
    {
        routeCountDownLatch = new Latch();
        AsynchronousUntilSuccessfulProcessingStrategy processingStrategy = createProcessingStrategy();
        processingStrategy.route(mockEvent);
    }

    private void configureMockRouteToCountDownRouteLatch() throws MuleException
    {
        when(mockRoute.process(any(MuleEvent.class))).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                routeCountDownLatch.countDown();
                if (failRoute)
                {
                    throw new RuntimeException("expected failure");
                }
                return invocationOnMock.getArguments()[0];
            }
        });
    }

    private void configureMockScheduledPoolToInvokeRunnableInNewThread()
    {
        when(mockScheduledPool.schedule(any(Callable.class), anyLong(), any(TimeUnit.class))).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable
            {
                assertThat((Long) invocationOnMock.getArguments()[1], is(mockUntilSuccessfulConfiguration.getMillisBetweenRetries()));
                assertThat((TimeUnit) invocationOnMock.getArguments()[2], is(TimeUnit.MILLISECONDS));
                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            ((Callable) invocationOnMock.getArguments()[0]).call();
                        }
                        catch (Exception e)
                        {
                            //Do nothing.
                        }
                    }
                }).start();
                return null;
            }
        });
    }

    private void waitUntilRouteIsExecuted() throws InterruptedException
    {
        if (!routeCountDownLatch.await(2000, TimeUnit.MILLISECONDS))
        {
            fail("route should be executed " + routeCountDownLatch.getCount() + " times");
        }
    }

    private AsynchronousUntilSuccessfulProcessingStrategy createProcessingStrategy() throws Exception
    {
        AsynchronousUntilSuccessfulProcessingStrategy processingStrategy = new AsynchronousUntilSuccessfulProcessingStrategy()
        {
            @Override
            protected MuleEvent threadSafeCopy(MuleEvent event)
            {
                return event;
            }
        };
        processingStrategy.setUntilSuccessfulConfiguration(mockUntilSuccessfulConfiguration);
        processingStrategy.setMessagingExceptionHandler(mockEvent.getFlowConstruct().getExceptionListener());
        processingStrategy.initialise();
        processingStrategy.start();
        return processingStrategy;
    }

    private void waitUntilExceptionStrategyIsExecuted() throws InterruptedException
    {
        if (!exceptionStrategyLatch.await(1000, TimeUnit.MILLISECONDS))
        {
            fail("exception strategy should be executed");
        }
    }

    private void configureExceptionStrategyToReleaseLatchWhenExecuted()
    {
        when(mockEvent.getFlowConstruct().getExceptionListener().handleException(any(Exception.class), any(MuleEvent.class))).thenAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                exceptionStrategyLatch.release();
                return null;
            }
        });
    }

}
