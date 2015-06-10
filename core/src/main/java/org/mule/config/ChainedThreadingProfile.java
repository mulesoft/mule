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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * This was written (perhaps too far in advance) with an eye to how we will manage default values
 * in a dynamic environment.  Since very little has been decided in that direction the correct
 * behaviour is unclear - changing the default value of "dynamic"
 * {@link org.mule.config.ChainedThreadingProfile#ChainedThreadingProfile(ThreadingProfile)}
 * will switch behaviour between dynamic and static chaining.
 *
 * <p>Note that within Spring, as far as I understand things, object creation is always ordered
 * so that dependencies are correctly resolved.  In that case, in a static scenario (or in a
 * dynamic one that rebuilds the instances) dynamic and static behaviour should be identical.</p>
 *
 * <p>Also, the "lazy" chaining is an optimisation - all hierarchies should be grounded in a final
 * default which is {@link ImmutableThreadingProfile} and, as such, return reliable
 * values (lazy would be necessary if this is not the case, since we must avoid evaluating
 * incomplete delegates).</p>
 */
public class ChainedThreadingProfile implements ThreadingProfile
{

    private Integer maxThreadsActive;
    private Integer maxThreadsIdle;
    private Integer maxBufferSize;
    private Long threadTTL;
    private Long threadWaitTimeout;
    private Integer poolExhaustedAction;
    private Boolean doThreading;

    private ThreadPoolFactory poolFactory = ThreadPoolFactory.newInstance();
    private WorkManagerFactory workManagerFactory = new ImmutableThreadingProfile.DefaultWorkManagerFactory();
    private RejectedExecutionHandler rejectedExecutionHandler;
    private ThreadFactory threadFactory;

    private ThreadingProfile delegate;

    private MuleContext muleContext;

    /**
     * Generate a mutable threading profile with fixed default values taken from
     * {@link #DEFAULT_THREADING_PROFILE}
     */
    public ChainedThreadingProfile()
    {
        // the default is itself constant, so dynanmic=true irrelevant
        this(DEFAULT_THREADING_PROFILE);
    }

    /**
     * Generate a mutable threading profile with dynamic default values taken from the
     * given delegate.
     *
     */
    public ChainedThreadingProfile(ThreadingProfile delegate)
    {
        this(delegate, true);
    }

    /**
     * Generate a mutable threading profile.  Default values are taken from the "delegate"
     * argument.  If dynamic is true then changes in the delegate instance are reflected in
     * this instance.
     * 
     * @param delegate Source of default values.
     * @param dynamic If true, changes in delegate are reflected in this instance
     */
    public ChainedThreadingProfile(ThreadingProfile delegate, boolean dynamic)
    {
        if (!dynamic)
        {
            // for static dependencies, we delegate to a fixed copy
            delegate = new ImmutableThreadingProfile(delegate);
        }
        this.delegate = delegate;
    }

    public int getMaxThreadsActive()
    {
        return null != maxThreadsActive ? maxThreadsActive : delegate.getMaxThreadsActive();
    }

    public int getMaxThreadsIdle()
    {
        return null != maxThreadsIdle ? maxThreadsIdle : delegate.getMaxThreadsIdle();
    }

    public long getThreadTTL()
    {
        return null != threadTTL ? threadTTL : delegate.getThreadTTL();
    }

    public long getThreadWaitTimeout()
    {
        return null != threadWaitTimeout ? threadWaitTimeout : delegate.getThreadWaitTimeout();
    }

    public int getPoolExhaustedAction()
    {
        return null != poolExhaustedAction ? poolExhaustedAction : delegate.getPoolExhaustedAction();
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

    public void setPoolExhaustedAction(int poolExhaustPolicy)
    {
        this.poolExhaustedAction = poolExhaustPolicy;
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
        return null != maxBufferSize ? maxBufferSize : delegate.getMaxBufferSize();
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
        // we deliberately don't instantiate the chained profile as we just want a cloned copy, not recursion
        return workManagerFactory.createWorkManager(new ImmutableThreadingProfile(this), name, shutdownTimeout);
    }

    public ExecutorService createPool()
    {
        return createPool(null);
    }

    public ExecutorService createPool(String name)
    {
        // we deliberately don't instantiate the chained profile as we just want a cloned copy, not recursion
        return poolFactory.createPool(name, new ImmutableThreadingProfile(this));
    }

    public boolean isDoThreading()
    {
        return null != doThreading ? doThreading : delegate.isDoThreading();
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
        return poolFactory.createScheduledPool(name, new ImmutableThreadingProfile(this));
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;

        // propagate mule context
        if (this.workManagerFactory instanceof MuleContextAware)
        {
            ((MuleContextAware) workManagerFactory).setMuleContext(muleContext);
        }

        poolFactory.setMuleContext(muleContext);
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

}
