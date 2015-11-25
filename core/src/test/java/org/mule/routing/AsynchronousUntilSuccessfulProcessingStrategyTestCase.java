/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
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
import static org.mule.routing.UntilSuccessful.DEFAULT_PROCESS_ATTEMPT_COUNT_PROPERTY_VALUE;
import static org.mule.routing.UntilSuccessful.PROCESS_ATTEMPT_COUNT_PROPERTY_NAME;

import org.mule.api.ExceptionPayload;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.retry.RetryPolicyExhaustedException;
import org.mule.routing.filters.ExpressionFilter;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.concurrent.Latch;
import org.mule.util.store.SimpleMemoryObjectStore;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
public class AsynchronousUntilSuccessfulProcessingStrategyTestCase extends AbstractMuleTestCase
{

    private static interface FailCallback
    {
        void doFail() throws Exception;
    }

    private static final String EXPECTED_FAILURE_MSG = "expected failure";
    private static final int DEFAULT_RETRIES = 4;
    private static final int DEFAULT_TRIES = DEFAULT_RETRIES + 1;

    private final Latch exceptionHandlingLatch = new Latch();
    private UntilSuccessfulConfiguration mockUntilSuccessfulConfiguration = mock(UntilSuccessfulConfiguration.class, Answers.RETURNS_DEEP_STUBS.get());
    private MuleEvent mockEvent = mock(MuleEvent.class, Answers.RETURNS_DEEP_STUBS.get());
    private MessageProcessor mockRoute = mock(MessageProcessor.class, Answers.RETURNS_DEEP_STUBS.get());
    private ExpressionFilter mockAlwaysTrueFailureExpressionFilter = mock(ExpressionFilter.class, Answers.RETURNS_DEEP_STUBS.get());
    private ThreadPoolExecutor mockPool = mock(ThreadPoolExecutor.class, Answers.RETURNS_DEEP_STUBS.get());
    private ScheduledThreadPoolExecutor mockScheduledPool = mock(ScheduledThreadPoolExecutor.class, Answers.RETURNS_DEEP_STUBS.get());
    private SimpleMemoryObjectStore<MuleEvent> objectStore = new SimpleMemoryObjectStore<MuleEvent>();
    private MessageProcessor mockDLQ = mock(MessageProcessor.class);
    private FailCallback failRoute = new FailCallback()
    {
        @Override
        public void doFail() throws MessagingException
        {
        }
    };
    private CountDownLatch routeCountDownLatch;

    @Before
    public void setUp() throws Exception
    {
        when(mockAlwaysTrueFailureExpressionFilter.accept(any(MuleMessage.class))).thenReturn(true);
        when(mockUntilSuccessfulConfiguration.getRoute()).thenReturn(mockRoute);
        when(mockUntilSuccessfulConfiguration.getAckExpression()).thenReturn(null);
        when(mockUntilSuccessfulConfiguration.getMaxRetries()).thenReturn(DEFAULT_RETRIES);
        // http://stackoverflow.com/questions/10324063/mockito-classcastexception
        when((Object) (mockEvent.getMessage().getInvocationProperty(PROCESS_ATTEMPT_COUNT_PROPERTY_NAME, DEFAULT_PROCESS_ATTEMPT_COUNT_PROPERTY_VALUE))).thenAnswer(new Answer<Object>()
        {
            private int numberOfAttempts = 0;

            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
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
        executeUntilSuccessfulFailingRoute(new FailCallback()
        {
            @Override
            public void doFail()
            {
                throw new RuntimeException(EXPECTED_FAILURE_MSG);
            }
        });
        waitUntilRouteIsExecuted();
    }

    @Test
    public void alwaysFailUsingFailureExpression() throws Exception
    {
        when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(null);
        when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
        executeUntilSuccessfulFailingRoute(new FailCallback()
        {
            @Override
            public void doFail()
            {
                throw new RuntimeException(EXPECTED_FAILURE_MSG);
            }
        });
        waitUntilRouteIsExecuted();
        waitUntilExceptionIsHandled();

        verify(mockEvent.getFlowConstruct().getExceptionListener(), times(1)).handleException(argThat(new ArgumentMatcher<Exception>()
        {
            @Override
            public boolean matches(Object item)
            {
                return item instanceof RetryPolicyExhaustedException && EXPECTED_FAILURE_MSG.equals(((RetryPolicyExhaustedException) item).getCause().getMessage());
            }
        }), eq(mockEvent));
        verify(mockDLQ, never()).process(any(MuleEvent.class));
    }

    @Test
    public void alwaysFailMessageUsingFailureExpression() throws Exception
    {
        when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(null);
        when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
        executeUntilSuccessfulFailingRoute(new FailCallback()
        {
            @Override
            public void doFail() throws MessagingException
            {
                throw new MessagingException(CoreMessages.createStaticMessage(EXPECTED_FAILURE_MSG), mockEvent, mockRoute);
            }
        });
        waitUntilRouteIsExecuted();
        waitUntilExceptionIsHandled();

        verify(mockEvent.getFlowConstruct().getExceptionListener(), times(1)).handleException(argThat(new ArgumentMatcher<Exception>()
        {
            @Override
            public boolean matches(Object item)
            {
                return item instanceof RetryPolicyExhaustedException && ((RetryPolicyExhaustedException) item).getMessage().contains("until-successful retries exhausted. Last exception message was: " + EXPECTED_FAILURE_MSG);
            }
        }), eq(mockEvent));
        verify(mockDLQ, never()).process(any(MuleEvent.class));
    }

    @Test
    public void alwaysFailMessageWrapUsingFailureExpression() throws Exception
    {
        when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(null);
        when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
        executeUntilSuccessfulFailingRoute(new FailCallback()
        {
            @Override
            public void doFail() throws MessagingException
            {
                throw new MessagingException(mockEvent, new RuntimeException(EXPECTED_FAILURE_MSG));
            }
        });
        waitUntilRouteIsExecuted();
        waitUntilExceptionIsHandled();

        verify(mockEvent.getFlowConstruct().getExceptionListener(), times(1)).handleException(argThat(new ArgumentMatcher<Exception>()
        {
            @Override
            public boolean matches(Object item)
            {
                return item instanceof RetryPolicyExhaustedException &&
                       ((RetryPolicyExhaustedException) item).getMessage().contains("until-successful retries exhausted. Last exception message was: " + EXPECTED_FAILURE_MSG);
            }
        }), eq(mockEvent));
        verify(mockDLQ, never()).process(any(MuleEvent.class));
    }

    @Test
    public void alwaysFailUsingFailureExpressionDLQ() throws Exception
    {
        when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(mockDLQ);
        when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
        executeUntilSuccessfulFailingRoute(new FailCallback()
        {
            @Override
            public void doFail()
            {
                throw new RuntimeException(EXPECTED_FAILURE_MSG);
            }
        });
        waitUntilRouteIsExecuted();
        waitUntilExceptionIsHandled();
        
        verify(mockEvent.getFlowConstruct().getExceptionListener(), never()).handleException(any(Exception.class), any(MuleEvent.class));
        verify(mockDLQ, times(1)).process(argThat(new ArgumentMatcher<MuleEvent>()
        {
            @Override
            public boolean matches(Object argument)
            {
                MuleEvent argEvent = (MuleEvent) argument;

                verify(argEvent.getMessage(), times(1)).setExceptionPayload(argThat(new ArgumentMatcher<ExceptionPayload>()
                {
                    @Override
                    public boolean matches(Object argument)
                    {
                        assertThat(((ExceptionPayload) argument).getException().getMessage(),
                                containsString("until-successful retries exhausted. Last exception message was: " + EXPECTED_FAILURE_MSG));
                        return true;
                    }
                }));

                return true;
            }
        }));
    }
    
    @Test
    public void alwaysFailMessageUsingFailureExpressionDLQ() throws Exception
    {
        when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(mockDLQ);
        when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
        executeUntilSuccessfulFailingRoute(new FailCallback()
        {
            @Override
            public void doFail() throws MessagingException
            {
                throw new MessagingException(CoreMessages.createStaticMessage(EXPECTED_FAILURE_MSG), mockEvent, mockRoute);
            }
        });
        waitUntilRouteIsExecuted();
        waitUntilExceptionIsHandled();
        
        verify(mockEvent.getFlowConstruct().getExceptionListener(), never()).handleException(any(Exception.class), any(MuleEvent.class));
        verify(mockDLQ, times(1)).process(argThat(new ArgumentMatcher<MuleEvent>()
        {
            @Override
            public boolean matches(Object argument)
            {
                MuleEvent argEvent = (MuleEvent) argument;

                verify(argEvent.getMessage(), times(1)).setExceptionPayload(argThat(new ArgumentMatcher<ExceptionPayload>()
                {
                    @Override
                    public boolean matches(Object argument)
                    {
                        assertThat(((ExceptionPayload) argument).getException().getMessage(),
                                containsString("until-successful retries exhausted. Last exception message was: " + EXPECTED_FAILURE_MSG));
                        return true;
                    }
                }));

                return true;
            }
        }));
    }
    
    @Test
    public void alwaysFailMessageWrapUsingFailureExpressionDLQ() throws Exception
    {
        when(mockUntilSuccessfulConfiguration.getDlqMP()).thenReturn(mockDLQ);
        when(mockUntilSuccessfulConfiguration.getFailureExpressionFilter()).thenReturn(mockAlwaysTrueFailureExpressionFilter);
        executeUntilSuccessfulFailingRoute(new FailCallback()
        {
            @Override
            public void doFail() throws MessagingException
            {
                throw new MessagingException(mockEvent, new RuntimeException(EXPECTED_FAILURE_MSG));
            }
        });
        waitUntilRouteIsExecuted();
        waitUntilExceptionIsHandled();
        
        verify(mockEvent.getFlowConstruct().getExceptionListener(), never()).handleException(any(Exception.class), any(MuleEvent.class));
        verify(mockDLQ, times(1)).process(argThat(new ArgumentMatcher<MuleEvent>()
        {
            @Override
            public boolean matches(Object argument)
            {
                MuleEvent argEvent = (MuleEvent)argument;
                
                verify(argEvent.getMessage(), times(1)).setExceptionPayload(argThat(new ArgumentMatcher<ExceptionPayload>()
                {
                    @Override
                    public boolean matches(Object argument)
                    {
                        assertThat(((ExceptionPayload) argument).getException().getMessage(),
                                containsString("until-successful retries exhausted. Last exception message was: " + EXPECTED_FAILURE_MSG));
                        return true;
                    }
                }));

                return true;
            }
        }));
    }
    
    @Test
    public void successfulExecution() throws Exception
    {
        executeUntilSuccessful();
        waitUntilRouteIsExecuted();
        verify(mockRoute, times(1)).process(mockEvent);
        verify(mockEvent.getFlowConstruct().getExceptionListener(), never()).handleException(any(Exception.class), eq(mockEvent));
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
        verify(mockEvent.getFlowConstruct().getExceptionListener(), never()).handleException(any(Exception.class), eq(mockEvent));
    }

    private void executeUntilSuccessfulFailingRoute(FailCallback failCallback) throws Exception
    {
        failRoute = failCallback;
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
                failRoute.doFail();
                return invocationOnMock.getArguments()[0];
            }
        });
    }

    private void configureMockPoolToInvokeRunnableInNewThread()
    {
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable
            {
                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        ((Runnable) invocationOnMock.getArguments()[0]).run();
                    }
                }).start();
                return null;
            }
        }).when(mockPool).execute(any(Runnable.class));
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
        if (!routeCountDownLatch.await(200000, TimeUnit.MILLISECONDS))
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

    private void waitUntilExceptionIsHandled() throws InterruptedException
    {
        if (!exceptionHandlingLatch.await(100000, TimeUnit.MILLISECONDS))
        {
            fail("exception should be handled");
        }
    }

    private void configureExceptionStrategyToReleaseLatchWhenExecuted()
    {
        when(mockEvent.getFlowConstruct().getExceptionListener().handleException(any(Exception.class), any(MuleEvent.class))).thenAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                exceptionHandlingLatch.release();
                return null;
            }
        });
    }

    private void configureDLQToReleaseLatchWhenExecuted() throws MuleException
    {
        when(mockDLQ.process(any(MuleEvent.class))).thenAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                exceptionHandlingLatch.release();
                return null;
            }
        });
    }

}
