/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.transport.MessageDispatcher;
import org.mule.runtime.core.config.ImmutableThreadingProfile;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestMessageDispatcher;
import org.mule.tck.testmodels.mule.TestMessageDispatcherFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * This test case tests the both dispatcher threading profile and it's rejection
 * handlers and AbstractConnector dispatch logic by dispatch events using
 * TestConnector with varying threading profile configurations and asserting the
 * correct outcome. See: MULE-4752
 */
public class DispatcherThreadingProfileTestCase extends AbstractMuleContextTestCase
{

    public static int DELAY_TIME = 500;
    public static int WAIT_TIME = DELAY_TIME + DELAY_TIME / 4;
    public static int SERIAL_WAIT_TIME = (DELAY_TIME * 2) + DELAY_TIME / 4;
    public static int LONGER_WAIT_TIME = DELAY_TIME * 5;
    private CountDownLatch latch;
    private AtomicInteger counter = new AtomicInteger();

    public DispatcherThreadingProfileTestCase()
    {
        setStartContext(true);
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        counter.set(0);
    }

    @Test
    public void testDefaultThreadingProfileConfiguration() throws MuleException
    {
        TestConnector connector = new TestConnector(muleContext);
        muleContext.getRegistry().registerConnector(connector);
        assertEquals(ThreadingProfile.DEFAULT_MAX_THREADS_ACTIVE, connector.getDispatcherThreadingProfile()
            .getMaxThreadsActive());
        assertEquals(ThreadingProfile.DEFAULT_MAX_THREADS_IDLE, connector.getDispatcherThreadingProfile()
            .getMaxThreadsIdle());
        assertEquals(ThreadingProfile.WHEN_EXHAUSTED_RUN, connector.getDispatcherThreadingProfile()
            .getPoolExhaustedAction());
        assertEquals(ThreadingProfile.DEFAULT_MAX_BUFFER_SIZE, connector.getDispatcherThreadingProfile()
            .getMaxBufferSize());
        assertEquals(ThreadingProfile.DEFAULT_MAX_THREAD_TTL, connector.getDispatcherThreadingProfile()
            .getThreadTTL());
        assertEquals(ThreadingProfile.DEFAULT_THREAD_WAIT_TIMEOUT, connector.getDispatcherThreadingProfile()
            .getThreadWaitTimeout());
    }

    @Test
    public void testDefaultRunExhaustedAction() throws Exception
    {
        // Default is RUN.
        // To concurrent dispatch operations are possible even with
        // maxActiveThreads=1.
        // Note: This also tests the fact with RUN here, the dispatcher pool needs to
        // GROW on demand.
        latch = new CountDownLatch(2);

        createTestConnectorWithSingleDispatcherThread(ThreadingProfile.WHEN_EXHAUSTED_RUN);
        dispatchTwoAsyncEvents();

        // Both execute complete at the same time and finish shortly after DELAY_TIME
        assertTrue(latch.await(WAIT_TIME, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testWaitExhaustedAction() throws Exception
    {
        // Second job waits in workQueue for first job to complete.
        latch = new CountDownLatch(2);

        createTestConnectorWithSingleDispatcherThread(1, ThreadingProfile.WHEN_EXHAUSTED_WAIT,
            ThreadingProfile.DEFAULT_THREAD_WAIT_TIMEOUT, ThreadingProfile.DEFAULT_MAX_BUFFER_SIZE);
        dispatchTwoAsyncEvents();

        // Both execute in serial as the second job wait for the fist job to complete
        assertTrue(latch.await(SERIAL_WAIT_TIME, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testWaitTimeoutExhaustedAction() throws Exception
    {
        // Second job attempts to wait in workQueue but waiting job times out/
        latch = new CountDownLatch(1);

        createTestConnectorWithSingleDispatcherThread(ThreadingProfile.WHEN_EXHAUSTED_WAIT);
        dispatchTwoAsyncEvents();

        // The job that executes finishes shortly after DELAY_TIME
        assertTrue(latch.await(WAIT_TIME, TimeUnit.MILLISECONDS));

        // Wait even longer and ensure the other message isn't executed.
        Thread.sleep(LONGER_WAIT_TIME);
        assertEquals(1, counter.get());
    }

    @Test
    public void testAbortExhaustedAction() throws Exception
    {
        // Second job is aborted
        latch = new CountDownLatch(1);

        createTestConnectorWithSingleDispatcherThread(ThreadingProfile.WHEN_EXHAUSTED_ABORT);
        dispatchTwoAsyncEvents();

        // The job that executes finishes shortly after DELAY_TIME
        assertTrue(latch.await(WAIT_TIME, TimeUnit.MILLISECONDS));

        // Wait even longer and ensure the other message isn't executed.
        Thread.sleep(LONGER_WAIT_TIME);
        assertEquals(1, counter.get());
    }

    @Test
    public void testDiscardExhaustedAction() throws Exception
    {
        // Second job is discarded
        latch = new CountDownLatch(1);

        createTestConnectorWithSingleDispatcherThread(ThreadingProfile.WHEN_EXHAUSTED_DISCARD);
        dispatchTwoAsyncEvents();

        // The job that executes finishes shortly after DELAY_TIME
        assertTrue(latch.await(WAIT_TIME, TimeUnit.MILLISECONDS));

        // Wait even longer and ensure the other message isn't executed.
        Thread.sleep(LONGER_WAIT_TIME);
        assertEquals(1, counter.get());
    }

    @Test
    public void testDiscardOldestExhaustedAction() throws Exception
    {
        // The third job is discarded when the fourth job is submitted
        // The fourth job (now third) is discarded when the fifth job is submitted.
        // The fifth job (now third) is discarded when the sixth job is submitted.
        // Therefore the first, second and sixth jobs are run
        latch = new CountDownLatch(3);

        // In order for a LinkedBlockingDeque to be used rather than a
        // SynchronousQueue there need to be
        // i) 2+ maxActiveThreads ii) maxBufferSize>0
        createTestConnectorWithSingleDispatcherThread(2, ThreadingProfile.WHEN_EXHAUSTED_DISCARD_OLDEST,
            ThreadingProfile.DEFAULT_THREAD_WAIT_TIMEOUT, 1);

        dispatchTwoAsyncEvents();
        dispatchTwoAsyncEvents();
        dispatchTwoAsyncEvents();

        assertTrue(latch.await(SERIAL_WAIT_TIME, TimeUnit.MILLISECONDS));
        Thread.sleep(LONGER_WAIT_TIME);
        assertEquals(3, counter.get());
    }

    protected void createTestConnectorWithSingleDispatcherThread(int exhaustedAction) throws MuleException
    {
        createTestConnectorWithSingleDispatcherThread(1, exhaustedAction, 1, 1);
    }

    protected void createTestConnectorWithSingleDispatcherThread(int threads,
                                                                 int exhaustedAction,
                                                                 long waitTimeout,
                                                                 int maxBufferSize) throws MuleException
    {
        TestConnector connector = new TestConnector(muleContext);
        ThreadingProfile threadingProfile = new ImmutableThreadingProfile(threads, threads, maxBufferSize,
            ThreadingProfile.DEFAULT_MAX_THREAD_TTL, waitTimeout, exhaustedAction, true, null, null);
        threadingProfile.setMuleContext(muleContext);
        connector.setDispatcherThreadingProfile(threadingProfile);
        muleContext.getRegistry().registerConnector(connector);
        connector.setDispatcherFactory(new DelayTestMessageDispatcherFactory());
    }

    private void dispatchTwoAsyncEvents() throws Exception
    {
        OutboundEndpoint endpoint = muleContext.getEndpointFactory().getOutboundEndpoint(
                "test://test");

        endpoint.process(getTestEvent("data", getTestInboundEndpoint(MessageExchangePattern.ONE_WAY)));
        endpoint.process(getTestEvent("data", getTestInboundEndpoint(MessageExchangePattern.ONE_WAY)));
    }

    public class DelayTestMessageDispatcher extends TestMessageDispatcher
    {
        public DelayTestMessageDispatcher(OutboundEndpoint endpoint)
        {
            super(endpoint);
        }

        @Override
        protected void doDispatch(MuleEvent event) throws Exception
        {
            new Exception().printStackTrace();
            super.doDispatch(event);
            Thread.sleep(DELAY_TIME);
            counter.incrementAndGet();
            latch.countDown();
        }
    }

    class DelayTestMessageDispatcherFactory extends TestMessageDispatcherFactory
    {
        @Override
        public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
        {
            return new DelayTestMessageDispatcher(endpoint);
        }
    }

}
