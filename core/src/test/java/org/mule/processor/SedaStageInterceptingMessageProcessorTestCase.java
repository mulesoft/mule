/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.api.config.ThreadingProfile.WHEN_EXHAUSTED_WAIT;
import static org.mule.processor.SedaStageInterceptingMessageProcessor.DEFAULT_QUEUE_SIZE_MAX_THREADS_FACTOR;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.config.MuleProperties;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.notification.AsyncMessageNotificationListener;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.service.FailedToQueueEventException;
import org.mule.config.ChainedThreadingProfile;
import org.mule.config.QueueProfile;
import org.mule.construct.Flow;
import org.mule.context.notification.AsyncMessageNotification;
import org.mule.management.stats.QueueStatistics;
import org.mule.processor.strategy.AsynchronousProcessingStrategy;
import org.mule.service.Pausable;
import org.mule.tck.MuleTestUtils;
import org.mule.util.concurrent.Latch;

import java.beans.ExceptionListener;
import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
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
    public void defaultQueueSize() throws Exception
    {
        SedaStageInterceptingMessageProcessor mp = createAsyncInterceptingMessageProcessor(target);
        assertThat(mp.queueProfile.getMaxOutstandingMessages(), is(equalTo(muleContext.getDefaultThreadingProfile()
                                                                                   .getMaxThreadsActive() *
                                                                           DEFAULT_QUEUE_SIZE_MAX_THREADS_FACTOR)));
    }

    @Test
    public void customQueueSize() throws Exception
    {
        int queueSize = 100;
        queueProfile.setMaxOutstandingMessages(queueSize);
        SedaStageInterceptingMessageProcessor mp;
        mp = new SedaStageInterceptingMessageProcessor("name", "name", queueProfile, queueTimeout,
                                                       muleContext.getDefaultThreadingProfile(), queueStatistics,
                                                       muleContext);
        assertThat(mp.queueProfile.getMaxOutstandingMessages(), is(equalTo(queueSize)));
    }

    @Test
    public void defaultQueueSizeCustomMaxThreads() throws Exception
    {
        int maxThreads = 200;
        ThreadingProfile tp = new ChainedThreadingProfile();
        tp.setMaxThreadsActive(maxThreads);
        SedaStageInterceptingMessageProcessor mp;
        mp = new SedaStageInterceptingMessageProcessor("name", "name", queueProfile, queueTimeout, tp,
                                                       queueStatistics, muleContext);
        assertThat(mp.queueProfile.getMaxOutstandingMessages(), is(equalTo(muleContext.getDefaultThreadingProfile()
                                                                                   .getMaxThreadsActive() *
                                                                           DEFAULT_QUEUE_SIZE_MAX_THREADS_FACTOR)));
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
        threadingProfile.setPoolExhaustedAction(WHEN_EXHAUSTED_WAIT);
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
    public void testProcessOneWayNegativeThreadWaitTimeout() throws Exception
    {
        ThreadingProfile threadingProfile = new ChainedThreadingProfile(muleContext.getDefaultThreadingProfile());
        threadingProfile.setThreadWaitTimeout(-1);
        // Need 2 threads: 1 for polling, 1 to process work
        threadingProfile.setMaxThreadsActive(2);
        threadingProfile.setPoolExhaustedAction(WHEN_EXHAUSTED_WAIT);
        threadingProfile.setMuleContext(muleContext);

        queueProfile.setMaxOutstandingMessages(1);

        final Latch latch = new Latch();
        MessageProcessor mockListener = mock(MessageProcessor.class);
        when(mockListener.process((MuleEvent)any())).thenAnswer(new Answer<MuleEvent>()
        {
            public MuleEvent answer(InvocationOnMock invocation) throws Throwable
            {
                // Await on latch so that only thread available is busy and otehr events have be be queued.
                latch.await();
                return (MuleEvent)invocation.getArguments()[0];
            }
        });

        SedaStageInterceptingMessageProcessor sedaStageInterceptingMessageProcessor = new SedaStageInterceptingMessageProcessor(
            "testProcessOneWayNegativeThreadWaitTimeout", "testProcessOneWayNegativeThreadWaitTimeout", queueProfile,
            queueTimeout, threadingProfile, queueStatistics, muleContext);
        sedaStageInterceptingMessageProcessor.setListener(mockListener);
        sedaStageInterceptingMessageProcessor.initialise();
        sedaStageInterceptingMessageProcessor.start();

        MessagingExceptionHandler exceptionHandler = mock(MessagingExceptionHandler.class);
        Flow flow = mock(Flow.class);
        when(flow.getExceptionListener()).thenReturn(exceptionHandler);
        when(flow.getProcessingStrategy()).thenReturn(new AsynchronousProcessingStrategy());
        final MuleEvent event = getTestEvent(TEST_MESSAGE, flow, MessageExchangePattern.ONE_WAY);

        int numberOfEvents = 3;

        for (int i = 0; i < numberOfEvents; i++)
        {
            sedaStageInterceptingMessageProcessor.process(event);
        }

        // Release latch only after 220ms which is 20ms more than the default queue
        // timeout.  If the new custom queueTimeout of -1 (unlimited) wasn't being
        // used then event 3 would be rejected, as there is no room for it in the queue while event 1 processing is waiting on
        // the latch.
        Thread.sleep(220);
        latch.countDown();

        ArgumentMatcher<MuleEvent> notSameEvent = createNotSameEventArgumentMatcher(event);

        // Three events are processed
        verify(mockListener, timeout(RECEIVE_TIMEOUT).times(numberOfEvents)).process(argThat(notSameEvent));
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

    @Override
    protected SedaStageInterceptingMessageProcessor createAsyncInterceptingMessageProcessor(MessageProcessor listener)
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

    @Test(expected = FailedToQueueEventException.class, timeout = 200)
    public void enqueueQueueFullThreadTimeout() throws Exception
    {
        ThreadingProfile threadingProfile = new ChainedThreadingProfile();
        threadingProfile.setThreadWaitTimeout(10);
        threadingProfile.setMuleContext(muleContext);
        // Create queue with capacity of 1, so that for second event queue is already full
        org.mule.api.store.QueueStore<Serializable> queueStore = (org.mule.api.store.QueueStore<Serializable>)
                muleContext.getRegistry().lookupObject(MuleProperties.QUEUE_STORE_DEFAULT_IN_MEMORY_NAME);
        QueueProfile queueProfile = new QueueProfile(1, queueStore);
        SedaStageInterceptingMessageProcessor mp;
        mp = new SedaStageInterceptingMessageProcessor("threadName", "queueMame", queueProfile, queueTimeout,
                                                       threadingProfile, queueStatistics, muleContext);
        mp.setListener(target);
        mp.initialise();
        // Don't start SedaStageInterceptingMessageProcessor to ensure events queue up and aren't removed from queue

        // First enqueue is successful as queue as max size of 1 defined.
        mp.enqueue(getTestEvent("foo"));

        // Second enqueue will cause thread to wait until timeout (10ms) and then throw a FailedToQueueEventException.
        mp.enqueue(getTestEvent("bar"));
    }

    @Test
    public void enqueueQueueSizeZero() throws Exception
    {
        // Simple check to ensure a zero queue size doesn't disable queue.
        org.mule.api.store.QueueStore<Serializable> queueStore = (org.mule.api.store.QueueStore<Serializable>)
                muleContext.getRegistry().lookupObject(MuleProperties.QUEUE_STORE_DEFAULT_IN_MEMORY_NAME);
        createMPAndQueueSingleEvent(new QueueProfile(0, queueStore));
    }

    @Test
    public void enqueueQueueSizeMinusOne() throws Exception
    {
        // Simple check to ensure a negative queue size doesn't cause an issues.
        org.mule.api.store.QueueStore<Serializable> queueStore = (org.mule.api.store.QueueStore<Serializable>)
                muleContext.getRegistry().lookupObject(MuleProperties.QUEUE_STORE_DEFAULT_IN_MEMORY_NAME);
        createMPAndQueueSingleEvent(new QueueProfile(-1, queueStore));
    }

    @Test
    public void testEventCopiedBeforeEnqueueInCallerThread() throws Exception
    {
        SedaStageInterceptingMessageProcessor sedaProcessor = (SedaStageInterceptingMessageProcessor) messageProcessor;

        MuleEvent testEvent = getTestEvent("", MessageExchangePattern.ONE_WAY);
        sedaProcessor.pause();
        messageProcessor.process(testEvent);

        MuleEvent queuedEvent = (MuleEvent) sedaProcessor.queue.poll(sedaProcessor.queueTimeout);

        assertThat(queuedEvent, is(notNullValue()));
        assertThat(queuedEvent, is(not(sameInstance(testEvent))));
    }

    @Test
    public void oneWayWithAsyncNotificationListener() throws Exception
    {
        CountDownLatch notificationLatch = new CountDownLatch(2);
        AsynMessageFiringNotificationListener listener = new AsynMessageFiringNotificationListener(notificationLatch);
        muleContext.registerListener(listener);

        MuleEvent event = MuleTestUtils.getTestEventUsingFlow(TEST_MESSAGE, MessageExchangePattern.ONE_WAY, muleContext);
        assertAsync(messageProcessor, event);
        notificationLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);

        // ASYNC_SCHEDULED receives same event instance as is queued
        assertThat(listener.asyncScheduledEvent, is(notNullValue()));
        assertThat(listener.asyncScheduledEvent, is(sameInstance(event)));
        assertThat(listener.asyncScheduledEvent, is(not(sameInstance(target.sensedEvent))));

        // ASYNC_COMPLETE receives same event passed to target processor
        assertThat(listener.asyncCompleteEvent, is(notNullValue()));
        assertThat(listener.asyncCompleteEvent, is(not(sameInstance(event))));
        assertThat(listener.asyncCompleteEvent, is(sameInstance(target.sensedEvent)));
    }

    @Test
    public void cleaningRequestContext() throws Exception
    {
        ThreadingProfile threadingProfile = new ChainedThreadingProfile(
                muleContext.getDefaultThreadingProfile());
        threadingProfile.setDoThreading(false);
        threadingProfile.setMuleContext(muleContext);

        SedaStageInterceptingMessageProcessor sedaStageInterceptingMessageProcessor = new SedaStageInterceptingMessageProcessor(
                "cleaningRequestContext", "cleaningRequestContext",
                queueProfile, queueTimeout, threadingProfile, queueStatistics, muleContext);

        MuleEvent testEvent = getTestEvent("", MessageExchangePattern.ONE_WAY);

        final TestLifeCycleState testState = new TestLifeCycleState()
        {
            int times = 0;
            @Override
            public boolean isStopped()
            {
                if (times > 0)
                {
                    return true;
                }
                times++;
                return super.isStopped();
            }
        };
        testState.initialise();
        testState.start();
        sedaStageInterceptingMessageProcessor.setLifecycleManager(new SedaStageLifecycleManager("cleaningRequestContext", sedaStageInterceptingMessageProcessor)
        {
            @Override
            public LifecycleState getState()
            {
                return testState;
            }
        });

        sedaStageInterceptingMessageProcessor.setListener(target);
        sedaStageInterceptingMessageProcessor.process(testEvent);
        sedaStageInterceptingMessageProcessor.run();
        assertNull(RequestContext.getEvent());

    }

    private class AsynMessageFiringNotificationListener implements AsyncMessageNotificationListener<AsyncMessageNotification>
    {

        CountDownLatch latch;
        MuleEvent asyncScheduledEvent;
        MuleEvent asyncCompleteEvent;

        public AsynMessageFiringNotificationListener(CountDownLatch latch)
        {
            this.latch = latch;
        }

        @Override
        public void onNotification(final AsyncMessageNotification notification)
        {
            if (notification.getAction() == AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED)
            {
                asyncScheduledEvent = (MuleEvent) notification.getSource();
                ((DefaultMuleMessage) asyncScheduledEvent.getMessage()).assertAccess(ThreadSafeAccess.WRITE);
            }
            else if (notification.getAction() == AsyncMessageNotification.PROCESS_ASYNC_COMPLETE)
            {
                asyncCompleteEvent = (MuleEvent) notification.getSource();
                ((DefaultMuleMessage) asyncCompleteEvent.getMessage()).assertAccess(ThreadSafeAccess.WRITE);
            }
            latch.countDown();
        }
    }

    private void createMPAndQueueSingleEvent(QueueProfile queueProfile) throws Exception
    {
        SedaStageInterceptingMessageProcessor mp;
        mp = new SedaStageInterceptingMessageProcessor("threadName", "queueMame", queueProfile, queueTimeout,
                                                       muleContext.getDefaultThreadingProfile(), queueStatistics,
                                                       muleContext);
        mp.setListener(target);
        mp.initialise();
        // Don't start SedaStageInterceptingMessageProcessor to ensure events queue up and aren't removed from queue

        // First enqueue is successful as queue as max size of 1 defined.
        mp.enqueue(getTestEvent("foo"));
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
