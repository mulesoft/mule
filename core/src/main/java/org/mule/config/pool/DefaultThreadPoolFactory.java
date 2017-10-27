/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.pool;

import static java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import static java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import static java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy;
import static java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import org.mule.api.config.ThreadingProfile;
import org.mule.util.StringUtils;
import org.mule.util.concurrent.NamedThreadFactory;
import org.mule.util.concurrent.WaitPolicy;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultThreadPoolFactory extends ThreadPoolFactory
{
    // deliberately shadow the superclass' static logger as to avoid log congestion on it
    protected final Log logger = LogFactory.getLog(getClass());

    @Override
    public ThreadPoolExecutor createPool(String name, ThreadingProfile tp)
    {
        BlockingQueue buffer;

        if (tp.getMaxBufferSize() > 0 && tp.getMaxThreadsActive() > 1)
        {
            buffer = new LinkedBlockingDeque(tp.getMaxBufferSize());
        }
        else
        {
            buffer = new SynchronousQueue();
        }

        ThreadPoolExecutor pool = internalCreatePool(name, tp, buffer);
        configureThreadPoolExecutor(name, tp, pool);
        return pool;

    }

    private void configureThreadPoolExecutor(String name, ThreadingProfile tp, ThreadPoolExecutor pool)
    {
        configureThreadFactory(name, tp, pool);


        if (tp.getRejectedExecutionHandler() != null)
        {
            pool.setRejectedExecutionHandler(tp.getRejectedExecutionHandler());
        }
        else
        {
            switch (tp.getPoolExhaustedAction())
            {
                case ThreadingProfile.WHEN_EXHAUSTED_DISCARD_OLDEST :
                    pool.setRejectedExecutionHandler(retrieveDiscardOldestPolicyOf(pool.getQueue()));
                    break;
                case ThreadingProfile.WHEN_EXHAUSTED_RUN :
                    pool.setRejectedExecutionHandler(new CallerRunsPolicy());
                    break;
                case ThreadingProfile.WHEN_EXHAUSTED_ABORT :
                    pool.setRejectedExecutionHandler(new AbortPolicy());
                    break;
                case ThreadingProfile.WHEN_EXHAUSTED_DISCARD :
                    pool.setRejectedExecutionHandler(new DiscardPolicy());
                    break;
                default :
                    // WHEN_EXHAUSTED_WAIT
                    pool.setRejectedExecutionHandler(new WaitPolicy(tp.getThreadWaitTimeout(), TimeUnit.MILLISECONDS));
                    break;
            }
        }
    }

    /**
     * DiscardOldestPolicy handler must discard the oldest task in the buffer queue. 
     * When used with a LinkedBlockingQueue or ArrayBlockingQueue, the oldest task
     * is the one that is first in line to execute when a thread becomes idle.
     * When used with a SynchronousQueue, there are never waiting task, so the submitted
     * task must be discarded, which is the same as using a DiscardPolicy.
     * 
     * @param queue buffer queue
     * 
     * @return appropriate rejection handler
     */
    private RejectedExecutionHandler retrieveDiscardOldestPolicyOf(BlockingQueue<Runnable> queue)
    {
        if (queue instanceof SynchronousQueue<?>)
        {
            return new DiscardPolicy();
        }

        return new DiscardOldestPolicy();
    }

    @Override
    public ScheduledThreadPoolExecutor createScheduledPool(String name, ThreadingProfile tp)
    {
        ScheduledThreadPoolExecutor pool = internalCreateScheduledPool(tp);
        configureThreadPoolExecutor(name, tp, pool);
        return pool;
    }

    protected void configureThreadFactory(String name, ThreadingProfile tp, ThreadPoolExecutor pool)
    {
        // use a custom ThreadFactory if one has been configured
        if (tp.getThreadFactory() != null)
        {
            pool.setThreadFactory(tp.getThreadFactory());
        }
        else
        {
            // ..else create a "NamedThreadFactory" if a proper name was passed in
            if (StringUtils.isNotBlank(name))
            {
                // Threads must use the MuleApplicationClassLoader related to MuleContext or the
                // thread context class loader in case of embedding mule.
                pool.setThreadFactory(new NamedThreadFactory(name, Thread.currentThread().getContextClassLoader()));
            }
            else
            {
                // let ThreadPoolExecutor create a default ThreadFactory;
                // see Executors.defaultThreadFactory()
            }
        }
    }

    protected ThreadPoolExecutor internalCreatePool(String name, ThreadingProfile tp, BlockingQueue buffer)
    {
        return new ThreadPoolExecutor(Math.min(tp.getMaxThreadsIdle(), tp.getMaxThreadsActive()),
                                      tp.getMaxThreadsActive(), tp.getThreadTTL(),
                                      TimeUnit.MILLISECONDS, buffer);
    }

    protected ScheduledThreadPoolExecutor internalCreateScheduledPool(ThreadingProfile tp)
    {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(tp.getMaxThreadsIdle());
        scheduledThreadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        scheduledThreadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(true);
        scheduledThreadPoolExecutor.setKeepAliveTime(tp.getThreadTTL(), TimeUnit.MILLISECONDS);
        return scheduledThreadPoolExecutor;
    }
}
