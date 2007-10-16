/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

import org.mule.impl.work.MuleWorkManager;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.util.concurrent.NamedThreadFactory;
import org.mule.util.concurrent.WaitPolicy;

import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingDeque;
import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionHandler;
import edu.emory.mathcs.backport.java.util.concurrent.SynchronousQueue;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadFactory;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;


public class ImmutableThreadingProfile implements ThreadingProfile
{

    private int maxThreadsActive;
    private int maxThreadsIdle;
    private int maxBufferSize;
    private long threadTTL;
    private long threadWaitTimeout;
    private int poolExhaustedAction;
    private boolean doThreading;

    private WorkManagerFactory workManagerFactory = new DefaultWorkManagerFactory();
    private RejectedExecutionHandler rejectedExecutionHandler;
    private ThreadFactory threadFactory;

    public ImmutableThreadingProfile(int maxThreadsActive,
                            int maxThreadsIdle,
                            int maxBufferSize,
                            long threadTTL,
                            long threadWaitTimeout,
                            int poolExhaustedAction,
                            boolean doThreading,
                            RejectedExecutionHandler rejectedExecutionHandler,
                            ThreadFactory threadFactory)
    {
        this.maxThreadsActive = maxThreadsActive;
        this.maxThreadsIdle = maxThreadsIdle;
        this.maxBufferSize = maxBufferSize;
        this.threadTTL = threadTTL;
        this.threadWaitTimeout = threadWaitTimeout;
        this.poolExhaustedAction = poolExhaustedAction;
        this.doThreading = doThreading;
        this.rejectedExecutionHandler = rejectedExecutionHandler;
        this.threadFactory = threadFactory;
    }

    public ImmutableThreadingProfile(ThreadingProfile tp)
    {
        this(tp.getMaxThreadsActive(),
                tp.getMaxThreadsIdle(),
                tp.getMaxBufferSize(),
                tp.getThreadTTL(),
                tp.getThreadWaitTimeout(),
                tp.getPoolExhaustedAction(),
                tp.isDoThreading(),
                tp.getRejectedExecutionHandler(),
                tp.getThreadFactory());
    }

    public int getMaxThreadsActive()
    {
        return maxThreadsActive;
    }

    public int getMaxThreadsIdle()
    {
        return maxThreadsIdle;
    }

    public long getThreadTTL()
    {
        return threadTTL;
    }

    public long getThreadWaitTimeout()
    {
        return threadWaitTimeout;
    }

    public int getPoolExhaustedAction()
    {
        return poolExhaustedAction;
    }

    public RejectedExecutionHandler getRejectedExecutionHandler()
    {
        return rejectedExecutionHandler;
    }

    public ThreadFactory getThreadFactory()
    {
        return threadFactory;
    }

    public void setMaxThreadsActive(int maxThreadsActive)
    {
        throw new UnsupportedOperationException(getClass().getName());
    }

    public void setMaxThreadsIdle(int maxThreadsIdle)
    {
        throw new UnsupportedOperationException(getClass().getName());
    }

    public void setThreadTTL(long threadTTL)
    {
        throw new UnsupportedOperationException(getClass().getName());
    }

    public void setThreadWaitTimeout(long threadWaitTimeout)
    {
        throw new UnsupportedOperationException(getClass().getName());
    }

    public void setPoolExhaustedAction(int poolExhaustPolicy)
    {
        throw new UnsupportedOperationException(getClass().getName());
    }

    public void setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler)
    {
        throw new UnsupportedOperationException(getClass().getName());
    }

    public void setThreadFactory(ThreadFactory threadFactory)
    {
        throw new UnsupportedOperationException(getClass().getName());
    }

    public int getMaxBufferSize()
    {
        return maxBufferSize;
    }

    public void setMaxBufferSize(int maxBufferSize)
    {
        throw new UnsupportedOperationException(getClass().getName());
    }

    public WorkManagerFactory getWorkManagerFactory()
    {
        return workManagerFactory;
    }

    public void setWorkManagerFactory(WorkManagerFactory workManagerFactory)
    {
        throw new UnsupportedOperationException(getClass().getName());
    }

    public UMOWorkManager createWorkManager(String name)
    {
        return workManagerFactory.createWorkManager(this, name);
    }

    public ThreadPoolExecutor createPool()
    {
        return createPool(null);
    }

    public ThreadPoolExecutor createPool(String name)
    {
        return createPool(name, this);
    }

    public boolean isDoThreading()
    {
        return doThreading;
    }

    public void setDoThreading(boolean doThreading)
    {
        throw new UnsupportedOperationException(getClass().getName());
    }

    public String toString()
    {
        return "ThreadingProfile{" + "maxThreadsActive=" + maxThreadsActive + ", maxThreadsIdle="
                        + maxThreadsIdle + ", maxBufferSize=" + maxBufferSize + ", threadTTL=" + threadTTL
                        + ", poolExhaustedAction=" + poolExhaustedAction + ", threadWaitTimeout="
                        + threadWaitTimeout + ", doThreading=" + doThreading + ", workManagerFactory="
                        + workManagerFactory + ", rejectedExecutionHandler=" + rejectedExecutionHandler
                        + ", threadFactory=" + threadFactory + "}";
    }

    public static class DefaultWorkManagerFactory implements WorkManagerFactory
    {

        public UMOWorkManager createWorkManager(ThreadingProfile profile, String name)
        {
            return new MuleWorkManager(profile, name);
        }

    }

    // this should be a separate factory, really
    public static ThreadPoolExecutor createPool(String name, ThreadingProfile tp)
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

        ThreadPoolExecutor pool =
                new ThreadPoolExecutor(Math.max(tp.getMaxThreadsIdle(), tp.getMaxThreadsActive()),
                        tp.getMaxThreadsActive(), tp.getThreadTTL(),
                        TimeUnit.MILLISECONDS, buffer);

        ThreadFactory tf = tp.getThreadFactory();
        if (name != null)
        {
            tf = new NamedThreadFactory(name);
        }

        if (tf != null)
        {
            pool.setThreadFactory(tf);
        }

        if (tp.getRejectedExecutionHandler() != null)
        {
            pool.setRejectedExecutionHandler(tp.getRejectedExecutionHandler());
        }
        else
        {
            switch (tp.getPoolExhaustedAction())
            {
                case WHEN_EXHAUSTED_DISCARD_OLDEST :
                    pool.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
                    break;
                case WHEN_EXHAUSTED_RUN :
                    pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
                    break;
                case WHEN_EXHAUSTED_ABORT :
                    pool.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
                    break;
                case WHEN_EXHAUSTED_DISCARD :
                    pool.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
                    break;
                default :
                    // WHEN_EXHAUSTED_WAIT
                    pool.setRejectedExecutionHandler(new WaitPolicy(tp.getThreadWaitTimeout(), TimeUnit.MILLISECONDS));
                    break;
            }
        }

        return pool;
    }

}