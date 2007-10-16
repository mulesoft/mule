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

import org.mule.umo.manager.UMOWorkManager;

import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionHandler;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadFactory;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;

/**
 * This was written (perhaps too far in advance) with an eye to how we will manage default values
 * in a dynamic environment.  Since very little has been decided in that direction the correct
 * behaviour is unclear - changing the default value of "dyanamic"
 * {@link org.mule.config.ChainedThreadingProfile#ChainedThreadingProfile(ThreadingProfile)}
 * will switch behaviour between dynamic and static chaining.
 *
 * <p>Note that within Spring, as far as I understand things, object creation is always ordered
 * so that dependencies are correctly resolved.  In that case, in a static scenario (or in a
 * dynamic one that rebuilds the instances) dyanmic and static behaviour should be identical.</p>
 *
 * <p>Also, the "lazy" chaining is an optimisation - all hierarchies should be grounded in a final
 * default which is {@link org.mule.config.ConstantThreadingProfile} and, as such, return reliable
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

    private WorkManagerFactory workManagerFactory = new ConstantThreadingProfile.DefaultWorkManagerFactory();
    private RejectedExecutionHandler rejectedExecutionHandler;
    private ThreadFactory threadFactory;

    private ThreadingProfile delegate;

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
     * @param delegate
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
            delegate = new ConstantThreadingProfile(delegate);
        }
        this.delegate = delegate;
    }

    public int getMaxThreadsActive()
    {
        return null != maxThreadsActive ? maxThreadsActive.intValue() : delegate.getMaxThreadsActive();
    }

    public int getMaxThreadsIdle()
    {
        return null != maxThreadsIdle ? maxThreadsIdle.intValue() :  delegate.getMaxThreadsIdle();
    }

    public long getThreadTTL()
    {
        return null != threadTTL ? threadTTL.longValue() : delegate.getThreadTTL();
    }

    public long getThreadWaitTimeout()
    {
        return null != threadWaitTimeout ? threadWaitTimeout.longValue() : delegate.getThreadWaitTimeout();
    }

    public int getPoolExhaustedAction()
    {
        return null != poolExhaustedAction ? poolExhaustedAction.intValue() : delegate.getPoolExhaustedAction();
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
        this.maxThreadsActive = new Integer(maxThreadsActive);
    }

    public void setMaxThreadsIdle(int maxThreadsIdle)
    {
        this.maxThreadsIdle = new Integer(maxThreadsIdle);
    }

    public void setThreadTTL(long threadTTL)
    {
        this.threadTTL = new Long(threadTTL);
    }

    public void setThreadWaitTimeout(long threadWaitTimeout)
    {
        this.threadWaitTimeout = new Long(threadWaitTimeout);
    }

    public void setPoolExhaustedAction(int poolExhaustPolicy)
    {
        this.poolExhaustedAction = new Integer(poolExhaustPolicy);
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
        return null != maxBufferSize ? maxBufferSize.intValue() : delegate.getMaxBufferSize();
    }

    public void setMaxBufferSize(int maxBufferSize)
    {
        this.maxBufferSize = new Integer(maxBufferSize);
    }

    public WorkManagerFactory getWorkManagerFactory()
    {
        return workManagerFactory;
    }

    public void setWorkManagerFactory(WorkManagerFactory workManagerFactory)
    {
        this.workManagerFactory = workManagerFactory;
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
        return ConstantThreadingProfile.createPool(name, this);
    }

    public boolean isDoThreading()
    {
        return null != doThreading ? doThreading.booleanValue() : delegate.isDoThreading();
    }

    public void setDoThreading(boolean doThreading)
    {
        this.doThreading = Boolean.valueOf(doThreading);
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