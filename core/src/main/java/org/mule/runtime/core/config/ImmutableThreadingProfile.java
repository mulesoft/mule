/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import org.mule.api.MuleContext;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.WorkManager;
import org.mule.config.pool.ThreadPoolFactory;
import org.mule.work.MuleWorkManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;


public class ImmutableThreadingProfile implements ThreadingProfile
{

    private int maxThreadsActive;
    private int maxThreadsIdle;
    private int maxBufferSize;
    private long threadTTL;
    private long threadWaitTimeout;
    private int poolExhaustedAction;
    private boolean doThreading;

    private ThreadPoolFactory poolFactory = ThreadPoolFactory.newInstance();
    private WorkManagerFactory workManagerFactory = new DefaultWorkManagerFactory();
    private RejectedExecutionHandler rejectedExecutionHandler;
    private ThreadFactory threadFactory;
    private MuleContext muleContext;

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

    public WorkManager createWorkManager(String name, int shutdownTimeout)
    {
        return workManagerFactory.createWorkManager(new ImmutableThreadingProfile(this), name, shutdownTimeout);
    }

    public ExecutorService createPool()
    {
        return createPool(null);
    }

    public ExecutorService createPool(String name)
    {
        return poolFactory.createPool(name, new ImmutableThreadingProfile(this));
    }

    public boolean isDoThreading()
    {
        return doThreading;
    }

    public void setDoThreading(boolean doThreading)
    {
        throw new UnsupportedOperationException(getClass().getName());
    }

    public ThreadPoolFactory getPoolFactory()
    {
        return poolFactory;
    }

    @Override
    public ScheduledExecutorService createScheduledPool(String name)
    {
        return poolFactory.createScheduledPool(name, new ImmutableThreadingProfile(this));
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;

        // propagate mule context
        if (this.workManagerFactory instanceof MuleContextAware)
        {
            ((MuleContextAware) workManagerFactory).setMuleContext(muleContext);
        }

        poolFactory.setMuleContext(muleContext);
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
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

    public static class DefaultWorkManagerFactory implements WorkManagerFactory, MuleContextAware
    {

        protected MuleContext muleContext;

        public WorkManager createWorkManager(ThreadingProfile profile, String name, int shutdownTimeout)
        {
            final WorkManager workManager = new MuleWorkManager(profile, name, shutdownTimeout);
            if (muleContext != null)
            {
                MuleContextAware contextAware = (MuleContextAware) workManager;
                contextAware.setMuleContext(muleContext);
            }

            return workManager;
        }

        public void setMuleContext(MuleContext context)
        {
            this.muleContext = context;
        }
    }

}
