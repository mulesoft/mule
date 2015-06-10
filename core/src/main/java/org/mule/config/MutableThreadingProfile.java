/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import org.mule.api.MuleContext;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManager;
import org.mule.config.pool.ThreadPoolFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Partial mutability for a threading profile. A typical use case for this class is to
 * copy an existing immutable threading profile via a copying constructor, modify values and
 * reconfigure a thread pool (if the latter supports it).<p/>
 * <p/>The following parameters are copied locally and can be modified directly:
 * <ul>
 *  <li>{@link #maxThreadsActive}
 *  <li>{@link #maxThreadsIdle}
 *  <li>{@link #maxBufferSize}
 *  <li>{@link #threadTTL}
 *  <li>{@link #threadWaitTimeout}
 *  <li>{@link #poolExhaustedAction}
 *  <li>{@link #doThreading}
 * </ul>
 * <p/>The following parameters re-use the same object reference as the original threading
 * profile and <strong>are not deep clones</strong> of those:
 * <ul>
 *  <li>{@link #poolFactory}
 *  <li>{@link #workManagerFactory}
 *  <li>{@link #rejectedExecutionHandler}
 *  <li>{@link #threadFactory}
 * </ul>
 */
public class MutableThreadingProfile implements ThreadingProfile
{

    private int maxThreadsActive;
    private int maxThreadsIdle;
    private int maxBufferSize;
    private long threadTTL;
    private long threadWaitTimeout;
    private int poolExhaustedAction;
    private boolean doThreading;

    private ThreadPoolFactory poolFactory;
    private WorkManagerFactory workManagerFactory;
    private RejectedExecutionHandler rejectedExecutionHandler;
    private ThreadFactory threadFactory;
    private MuleContext muleContext;

    public MutableThreadingProfile(ThreadingProfile tp)
    {
        this.maxThreadsActive = tp.getMaxThreadsActive();
        this.maxThreadsIdle = tp.getMaxThreadsIdle();
        this.maxBufferSize = tp.getMaxBufferSize();
        this.threadTTL = tp.getThreadTTL();
        this.threadWaitTimeout = tp.getThreadWaitTimeout();
        this.poolExhaustedAction = tp.getPoolExhaustedAction();
        this.doThreading = tp.isDoThreading();
        this.rejectedExecutionHandler = tp.getRejectedExecutionHandler();
        this.threadFactory = tp.getThreadFactory();
        this.workManagerFactory = tp.getWorkManagerFactory();
        this.poolFactory = tp.getPoolFactory();
    }

    public ExecutorService createPool()
    {
        return createPool(null);
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
        this.maxThreadsActive = maxThreadsActive;
    }

    public void setMaxThreadsIdle(int maxThreadsIdle)
    {
        this.maxThreadsIdle = maxThreadsIdle;
    }

    public void setThreadTTL(long threadTTL)
    {
        this.threadTTL = threadTTL;
    }

    public void setThreadWaitTimeout(long threadWaitTimeout)
    {
        this.threadWaitTimeout = threadWaitTimeout;
    }

    public void setPoolExhaustedAction(int poolExhaustedAction)
    {
        this.poolExhaustedAction = poolExhaustedAction;
    }

    public void setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler)
    {
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }

    public void setThreadFactory(ThreadFactory threadFactory)
    {
        this.threadFactory = threadFactory;
    }

    public int getMaxBufferSize()
    {
        return maxBufferSize;
    }

    public void setMaxBufferSize(int maxBufferSize)
    {
        this.maxBufferSize = maxBufferSize;
    }

    public WorkManagerFactory getWorkManagerFactory()
    {
        return workManagerFactory;
    }

    public void setWorkManagerFactory(WorkManagerFactory workManagerFactory)
    {
        this.workManagerFactory = workManagerFactory;
    }

    public WorkManager createWorkManager(String name, int shutdownTimeout)
    {
        return workManagerFactory.createWorkManager(new ImmutableThreadingProfile(this), name, shutdownTimeout);
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
        this.doThreading = doThreading;
    }

    public ThreadPoolFactory getPoolFactory()
    {
        return poolFactory;
    }

    @Override
    public ScheduledExecutorService createScheduledPool(String name)
    {
        return poolFactory.createScheduledPool(name,new ImmutableThreadingProfile(this));
    }

    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }
}
