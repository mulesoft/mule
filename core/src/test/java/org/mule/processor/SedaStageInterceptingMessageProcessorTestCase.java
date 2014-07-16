/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.MessageExchangePattern;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.ChainedThreadingProfile;
import org.mule.config.QueueProfile;
import org.mule.construct.Flow;
import org.mule.management.stats.QueueStatistics;
import org.mule.processor.strategy.AsynchronousProcessingStrategy;
import org.mule.service.Pausable;
import org.mule.util.concurrent.Latch;

import java.beans.ExceptionListener;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class SedaStageInterceptingMessageProcessorTestCase extends AsyncInterceptingMessageProcessorTestCase
    implements ExceptionListener
{
    QueueProfile queueProfile = null;
    int queueTimeout;
    QueueStatistics queueStatistics;
    TestLifeCycleState lifeCycleState;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        queueProfile = QueueProfile.newInstancePersistingToDefaultMemoryQueueStore(muleContext);
        queueStatistics = new TestQueueStatistics();
        queueTimeout = muleContext.getConfiguration().getDefaultQueueTimeout();
        lifeCycleState = new TestLifeCycleState();
        super.doSetUp();
        ((Initialisable)messageProcessor).initialise();
        ((Startable)messageProcessor).start();
        lifeCycleState.start();
    }

    @Override
    protected boolean isStartContext()
    {
        return false;
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        ((Stoppable)messageProcessor).stop();
        lifeCycleState.stop();
        lifeCycleState.dispose();

    }

    @Test
    public void testProcessOneWayThreadWaitTimeout() throws Exception
    {
        final int threadTimeout = 20;
        ThreadingProfile threadingProfile = new ChainedThreadingProfile(
            muleContext.getDefaultThreadingProfile());
        threadingProfile.setThreadWaitTimeout(threadTimeout);
        // Need 3 threads: 1 for polling, 2 to process work successfully without timeout
        threadingProfile.setMaxThreadsActive(3);
        threadingProfile.setPoolExhaustedAction(ThreadingProfile.WHEN_EXHAUSTED_WAIT);
        threadingProfile.setMuleContext(muleContext);

        MessageProcessor mockListener = mock(MessageProcessor.class);
        when(mockListener.process((MuleEvent)any())).thenAnswer(new Answer<MuleEvent>()
        {
            public MuleEvent answer(InvocationOnMock invocation) throws Throwable
            {
                Thread.sleep(threadTimeout * 2);
                return (MuleEvent)invocation.getArguments()[0];
            }
        });

        SedaStageInterceptingMessageProcessor sedaStageInterceptingMessageProcessor = new SedaStageInterceptingMessageProcessor(
            "testProcessOneWayThreadWaitTimeout", "testProcessOneWayThreadWaitTimeout", queueProfile,
            queueTimeout, threadingProfile, queueStatistics, muleContext);
        sedaStageInterceptingMessageProcessor.setListener(mockListener);
        sedaStageInterceptingMessageProcessor.initialise();
        sedaStageInterceptingMessageProcessor.start();

        MessagingExceptionHandler exceptionHandler = mock(MessagingExceptionHandler.class);
        Flow flow = mock(Flow.class);
        when(flow.getExceptionListener()).thenReturn(exceptionHandler);
        when(flow.getProcessingStrategy()).thenReturn(new AsynchronousProcessingStrategy());
        final MuleEvent event = getTestEvent(TEST_MESSAGE, flow, MessageExchangePattern.ONE_WAY);

        for (int i = 0; i < 3; i++)
        {
            sedaStageInterceptingMessageProcessor.process(event);
        }

        ArgumentMatcher<MuleEvent> notSameEvent = createNotSameEventArgumentMatcher(event);

        // Two events are processed
        verify(mockListener, timeout(RECEIVE_TIMEOUT).times(2)).process(argThat(notSameEvent));

        // One event gets processed by the exception strategy
        verify(exceptionHandler, timeout(RECEIVE_TIMEOUT).times(1)).handleException((Exception)any(),
            argThat(notSameEvent));

    }

    @Test
    public void testProcessOneWayWithException() throws Exception
    {
        final Latch latch = new Latch();
        ThreadingProfile threadingProfile = new ChainedThreadingProfile(
            muleContext.getDefaultThreadingProfile());
        threadingProfile.setMuleContext(muleContext);

        MessageProcessor mockListener = mock(MessageProcessor.class);
        when(mockListener.process((MuleEvent)any())).thenAnswer(new Answer<MuleEvent>()
        {
            public MuleEvent answer(InvocationOnMock invocation) throws Throwable
            {
                latch.countDown();
                throw new RuntimeException();
            }
        });

        SedaStageInterceptingMessageProcessor sedaStageInterceptingMessageProcessor = new SedaStageInterceptingMessageProcessor(
            "testProcessOneWayWithException", "testProcessOneWayWithException", queueProfile, queueTimeout,
            threadingProfile, queueStatistics, muleContext);
        sedaStageInterceptingMessageProcessor.setListener(mockListener);
        sedaStageInterceptingMessageProcessor.initialise();
        sedaStageInterceptingMessageProcessor.start();

        MessagingExceptionHandler exceptionHandler = mock(MessagingExceptionHandler.class);
        Flow flow = mock(Flow.class);
        when(flow.getExceptionListener()).thenReturn(exceptionHandler);
        when(flow.getProcessingStrategy()).thenReturn(new AsynchronousProcessingStrategy());
        final MuleEvent event = getTestEvent(TEST_MESSAGE, flow, MessageExchangePattern.ONE_WAY);

        sedaStageInterceptingMessageProcessor.process(event);

        assertTrue(latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));

        ArgumentMatcher<MuleEvent> notSameEvent = createNotSameEventArgumentMatcher(event);

        // One event get processed but then throws an exception
        verify(mockListener, timeout(RECEIVE_TIMEOUT).times(1)).process(argThat(notSameEvent));

        // One event gets processed by the exception strategy
        verify(exceptionHandler, timeout(RECEIVE_TIMEOUT).times(1)).handleException((Exception)any(),
            argThat(notSameEvent));

    }

    private ArgumentMatcher<MuleEvent> createNotSameEventArgumentMatcher(final MuleEvent event)
    {
        return new ArgumentMatcher<MuleEvent>()
        {
            @Override
            public boolean matches(Object argument)
            {
                return argument != event;
            }
        };
    }

    @Test(expected = MessagingException.class)
    public void testProcessOneWayNoThreadingWithException() throws Exception
    {
        ThreadingProfile threadingProfile = new ChainedThreadingProfile(
            muleContext.getDefaultThreadingProfile());
        threadingProfile.setDoThreading(false);
        threadingProfile.setMuleContext(muleContext);

        MessageProcessor mockListener = mock(MessageProcessor.class);
        when(mockListener.process((MuleEvent)any())).thenThrow(new RuntimeException());

        SedaStageInterceptingMessageProcessor sedaStageInterceptingMessageProcessor = new SedaStageInterceptingMessageProcessor(
            "testProcessOneWayNoThreadingWithException", "testProcessOneWayNoThreadingWithException",
            queueProfile, queueTimeout, threadingProfile, queueStatistics, muleContext);
        sedaStageInterceptingMessageProcessor.setListener(mockListener);
        sedaStageInterceptingMessageProcessor.initialise();
        sedaStageInterceptingMessageProcessor.start();

        MessagingExceptionHandler exceptionHandler = mock(MessagingExceptionHandler.class);
        Flow flow = mock(Flow.class);
        when(flow.getExceptionListener()).thenReturn(exceptionHandler);
        when(flow.getProcessingStrategy()).thenReturn(new AsynchronousProcessingStrategy());
        MuleEvent event = getTestEvent(TEST_MESSAGE, flow, MessageExchangePattern.ONE_WAY);

        sedaStageInterceptingMessageProcessor.process(event);
    }


    @Test
    public void testProcessAsyncDoCopyEvent() throws Exception
    {
        SedaStageInterceptingMessageProcessor sedaStageInterceptingMessageProcessor =
                new SedaStageInterceptingMessageProcessor(
                  "testProcessAsyncDoCopyEvent", "testProcessAsyncDoCopyEvent", queueProfile,
                  queueTimeout, mock(ThreadingProfile.class), queueStatistics, muleContext);

        sedaStageInterceptingMessageProcessor.setListener(mock(MessageProcessor.class));
        sedaStageInterceptingMessageProcessor.initialise();

        MessagingExceptionHandler exceptionHandler = mock(MessagingExceptionHandler.class);
        Flow flow = mock(Flow.class);
        when(flow.getExceptionListener()).thenReturn(exceptionHandler);
        when(flow.getProcessingStrategy()).thenReturn(new AsynchronousProcessingStrategy());
        final MuleEvent event = getTestEvent(TEST_MESSAGE, flow, MessageExchangePattern.ONE_WAY);

        sedaStageInterceptingMessageProcessor.processNextAsync(event);
        MuleEvent queuedEvent = sedaStageInterceptingMessageProcessor.dequeue();

        assertThat(queuedEvent, is(not(nullValue())));
        assertThat(queuedEvent, is(not(same(event))));
    }



    @Override
    protected AsyncInterceptingMessageProcessor createAsyncInterceptingMessageProcessor(MessageProcessor listener)
        throws Exception
    {
        SedaStageInterceptingMessageProcessor mp = new SedaStageInterceptingMessageProcessor("name", "name",
            queueProfile, queueTimeout, muleContext.getDefaultThreadingProfile(), queueStatistics,
            muleContext);
        mp.setMuleContext(muleContext);
        mp.setListener(listener);
        return mp;
    }

    @Test
    public void testSpiWorkThrowableHandling() throws Exception
    {
        try
        {
            new AsyncWorkListener(getSensingNullMessageProcessor()).handleWorkException(getTestWorkEvent(),
                "workRejected");
        }
        catch (MuleRuntimeException mrex)
        {
            assertNotNull(mrex);
            assertTrue(mrex.getCause().getClass() == Throwable.class);
            assertEquals("testThrowable", mrex.getCause().getMessage());
        }
    }

    private WorkEvent getTestWorkEvent()
    {
        return new WorkEvent(this, // source
            WorkEvent.WORK_REJECTED, getTestWork(), new WorkException(new Throwable("testThrowable")));
    }

    private Work getTestWork()
    {
        return new Work()
        {
            @Override
            public void release()
            {
                // noop
            }

            @Override
            public void run()
            {
                // noop
            }
        };
    }

    class TestQueueStatistics implements QueueStatistics
    {
        int incCount;
        int decCount;

        @Override
        public void decQueuedEvent()
        {
            decCount++;
        }

        @Override
        public void incQueuedEvent()
        {
            incCount++;
        }

        @Override
        public boolean isEnabled()
        {
            return true;
        }
    }

    class TestLifeCycleState implements LifecycleState, Lifecycle
    {

        AtomicBoolean started = new AtomicBoolean(false);
        AtomicBoolean stopped = new AtomicBoolean(true);
        AtomicBoolean disposed = new AtomicBoolean(false);
        AtomicBoolean initialised = new AtomicBoolean(false);
        AtomicBoolean paused = new AtomicBoolean(false);

        @Override
        public boolean isDisposed()
        {
            return disposed.get();
        }

        @Override
        public boolean isDisposing()
        {
            return false;
        }

        @Override
        public boolean isInitialised()
        {
            return initialised.get();
        }

        @Override
        public boolean isInitialising()
        {
            return false;
        }

        @Override
        public boolean isPhaseComplete(String phase)
        {
            if (Pausable.PHASE_NAME.equals(phase))
            {
                return paused.get();
            }
            else
            {
                return false;
            }
        }

        @Override
        public boolean isPhaseExecuting(String phase)
        {
            return false;
        }

        @Override
        public boolean isStarted()
        {
            return started.get();
        }

        @Override
        public boolean isStarting()
        {
            return false;
        }

        @Override
        public boolean isStopped()
        {
            return stopped.get();
        }

        @Override
        public boolean isStopping()
        {
            return false;
        }

        @Override
        public void initialise() throws InitialisationException
        {
            initialised.set(true);
        }

        @Override
        public void start() throws MuleException
        {
            initialised.set(false);
            stopped.set(false);
            started.set(true);
        }

        @Override
        public void stop() throws MuleException
        {
            started.set(false);
            stopped.set(true);
        }

        @Override
        public void dispose()
        {
            stopped.set(true);
            disposed.set(true);
        }

        @Override
        public boolean isValidTransition(String phase)
        {
            return false;
        }
    }
}
