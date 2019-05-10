/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.MessageProducer;
import javax.jms.Session;

import org.slf4j.Logger;

/**
 * Thread that runs concurrently with a JmsConnector, and is responsible for closing JMS resources, which are explicitly
 * deferred by the original close callee.
 * <p>
 * During the <b>{@link JmsConnector} doStart</b> call, this thread will be launched, with a reference to the {@link
 * BlockingQueue} from who to take closable resources. Since the queue is blocking, it will wait for elements to be
 * pushed to it. During the <b>doStop</b>, the stop phase will wait with a certain timeout for the queue to be emptied.
 * Either case, this thread will be interrupted, and proceed to it's shutdown.
 * </p>
 */
class DeferredJmsResourceCloser extends Thread
{

    private JmsConnector jmsConnector;
    private final Logger LOGGER = getLogger(DeferredJmsResourceCloser.class);
    private final JmsConnector connector;
    private final BlockingQueue<Object> queue;
    private Semaphore awaitForEmptyQueueSync = new Semaphore(0);
    private AtomicBoolean exitOnEmptyQueue = new AtomicBoolean(false);

    /**
     * @param jmsConnector       the connector to whom this thread belongs
     * @param deferredCloseQueue
     */
    DeferredJmsResourceCloser(JmsConnector jmsConnector, BlockingQueue<Object> deferredCloseQueue)
    {
        super("DeferredJMSResourcesCloser");
        this.jmsConnector = jmsConnector;
        this.connector = jmsConnector;
        this.queue = deferredCloseQueue;
    }


    @Override
    public void run()
    {
        // If thread is interrupted, it's because the connector is being stopped. Die
        while (!Thread.currentThread().isInterrupted()
               && !isTerminateRequested())
        {
            // If queue is empty, this locks waiting for next element
            Object closable = takeLoggingOnInterrupt();
            if (closable instanceof MessageProducer)
            {
                connector.closeQuietly((MessageProducer) closable);
            }
            else if (closable instanceof Session)
            {
                connector.closeQuietly((Session) closable);
            }
            else
            {
                // This case should represent a misuse, or that of closabe being null, which is caused by this thread being interrupted.
                LOGGER.warn("A JMS Resource of class {} was inserted in the deferred close queue, but wasn't able to be closed.", closable.getClass().getName());
            }
        }
        if (exitOnEmptyQueue.get())
        {
            awaitForEmptyQueueSync.release();
        }
    }

    private boolean isTerminateRequested()
    {
        return exitOnEmptyQueue.get() && queue.isEmpty();
    }

    /**
     * Tries to pop the element at the front of the queue, <b>blocking if it's empty</b>.
     *
     * @return the {@link Object} at the front of the queue, or <b>null</b> if it was interrupted while waiting
     */
    private Object takeLoggingOnInterrupt()
    {
        try
        {
            return queue.take();
        }
        catch (InterruptedException e)
        {
            LOGGER.warn("Thread was interrupted waiting for a resource to be deferred: ", e);
        }
        return null;
    }

    /**
     * Wait for closables queue to be emptied. If after @amount @unit time the close did not notify its termination,
     * this method will return.
     *
     * @param amount amount of time to wait
     * @param unit   time unit
     */
    public void waitForEmptyQueueOrTimeout(int amount, TimeUnit unit)
    {
        exitOnEmptyQueue.set(true);
        try
        {
            awaitForEmptyQueueSync.tryAcquire(amount, unit);
        }
        catch (InterruptedException e)
        {
            LOGGER.warn("Thread was interrupted while waiting for deferred-close-queue to be emptied: ", e);
        }
    }
}
