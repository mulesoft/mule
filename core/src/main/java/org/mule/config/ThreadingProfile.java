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

import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingDeque;
import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionHandler;
import edu.emory.mathcs.backport.java.util.concurrent.SynchronousQueue;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadFactory;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.apache.commons.collections.map.CaseInsensitiveMap;

/**
 * <code>ThreadingProfile</code> is used to configure a thread pool. Mule uses a
 * few different pools i.e. for component threads and message dispatchers. This object
 * makes it easier to configure the pool.
 */

public class ThreadingProfile
{

    /**
     * Default value for MAX_THREADS_ACTIVE
     */
    public static final int DEFAULT_MAX_THREADS_ACTIVE = 16;

    /**
     * Default value for MAX_THREADS_IDLE
     */
    public static final int DEFAULT_MAX_THREADS_IDLE = 1;

    /**
     * Default value for MAX_BUFFER_SIZE
     */
    public static final int DEFAULT_MAX_BUFFER_SIZE = 0;

    /**
     * Default value for MAX_THREAD_TTL
     */
    public static final long DEFAULT_MAX_THREAD_TTL = 60000;

    /**
     * Default value for DEFAULT_THREAD_WAIT_TIMEOUT
     */
    public static final long DEFAULT_THREAD_WAIT_TIMEOUT = 30000L;

    /**
     * Default value for do threading
     */
    public static final boolean DEFAULT_DO_THREADING = true;

    /**
     * Actions to perform on pool exhaustion
     */
    public static final int WHEN_EXHAUSTED_WAIT = 0;
    public static final int WHEN_EXHAUSTED_DISCARD = 1;
    public static final int WHEN_EXHAUSTED_DISCARD_OLDEST = 2;
    public static final int WHEN_EXHAUSTED_ABORT = 3;
    public static final int WHEN_EXHAUSTED_RUN = 4;

    /**
     * Default action to perform on pool exhaustion
     */
    public static final int DEFAULT_POOL_EXHAUST_ACTION = WHEN_EXHAUSTED_RUN;

    // map pool exhaustion strings to their respective values
    public static final Map POOL_EXHAUSTED_ACTIONS = new CaseInsensitiveMap()
    {
        private static final long serialVersionUID = 1L;

        // static initializer
        {
            Integer value = new Integer(WHEN_EXHAUSTED_WAIT);
            this.put("WHEN_EXHAUSTED_WAIT", value);
            this.put("WAIT", value);

            value = new Integer(WHEN_EXHAUSTED_DISCARD);
            this.put("WHEN_EXHAUSTED_DISCARD", value);
            this.put("DISCARD", value);

            value = new Integer(WHEN_EXHAUSTED_DISCARD_OLDEST);
            this.put("WHEN_EXHAUSTED_DISCARD_OLDEST", value);
            this.put("DISCARD_OLDEST", value);

            value = new Integer(WHEN_EXHAUSTED_ABORT);
            this.put("WHEN_EXHAUSTED_ABORT", value);
            this.put("ABORT", value);

            value = new Integer(WHEN_EXHAUSTED_RUN);
            this.put("WHEN_EXHAUSTED_RUN", value);
            this.put("RUN", value);
        }
    };

    private static ThreadingProfile DEFAULT_THREADING_PROFILE =
            new ThreadingProfile(
                    DEFAULT_MAX_THREADS_ACTIVE,
                    DEFAULT_MAX_THREADS_IDLE,
                    DEFAULT_MAX_BUFFER_SIZE,
                    DEFAULT_MAX_THREAD_TTL,
                    DEFAULT_THREAD_WAIT_TIMEOUT,
                    DEFAULT_POOL_EXHAUST_ACTION,
                    DEFAULT_DO_THREADING,
                    null,
                    null
                    );

    private Integer maxThreadsActive;
    private Integer maxThreadsIdle;
    private Integer maxBufferSize;
    private Long threadTTL;
    private Long threadWaitTimeout;
    private Integer poolExhaustedAction;
    private Boolean doThreading;

    private ThreadingProfile delegate = DEFAULT_THREADING_PROFILE;

    private WorkManagerFactory workManagerFactory = new DefaultWorkManagerFactory();
    private RejectedExecutionHandler rejectedExecutionHandler;
    private ThreadFactory threadFactory;

    public ThreadingProfile()
    {
        // use defaults
    }

    public ThreadingProfile(int maxThreadsActive,
                            int maxThreadsIdle,
                            int maxBufferSize,
                            long threadTTL,
                            long threadWaitTimeout,
                            int poolExhaustedAction,
                            boolean doThreading,
                            RejectedExecutionHandler rejectedExecutionHandler,
                            ThreadFactory threadFactory)
    {
        setMaxThreadsActive(maxThreadsActive);
        setMaxThreadsIdle(maxThreadsIdle);
        setMaxBufferSize(maxBufferSize);
        setThreadTTL(threadTTL);
        setThreadWaitTimeout(threadWaitTimeout);
        setPoolExhaustedAction(poolExhaustedAction);
        setDoThreading(doThreading);
        setRejectedExecutionHandler(rejectedExecutionHandler);
        setThreadFactory(threadFactory);
    }

    public ThreadingProfile(ThreadingProfile tp)
    {
        delegate = tp;
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
        BlockingQueue buffer;

        if (getMaxBufferSize() > 0 && getMaxThreadsActive() > 1)
        {
            buffer = new LinkedBlockingDeque(getMaxBufferSize());
        }
        else
        {
            buffer = new SynchronousQueue();
        }

        ThreadPoolExecutor pool =
                new ThreadPoolExecutor(Math.max(getMaxThreadsIdle(), getMaxThreadsActive()),
                        getMaxThreadsActive(), getThreadTTL(),
                        TimeUnit.MILLISECONDS, buffer);

        ThreadFactory tf = threadFactory;
        if (name != null)
        {
            tf = new NamedThreadFactory(name); 
        }

        if (tf != null)
        {
            pool.setThreadFactory(tf);
        }

        if (rejectedExecutionHandler != null)
        {
            pool.setRejectedExecutionHandler(rejectedExecutionHandler);
        }
        else
        {
            switch (getPoolExhaustedAction())
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
                    pool.setRejectedExecutionHandler(new WaitPolicy(getThreadWaitTimeout(), TimeUnit.MILLISECONDS));
                    break;
            }
        }

        return pool;
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

    public static interface WorkManagerFactory
    {
        UMOWorkManager createWorkManager(ThreadingProfile profile, String name);
    }

    private static class DefaultWorkManagerFactory implements WorkManagerFactory
    {
        public UMOWorkManager createWorkManager(ThreadingProfile profile, String name)
        {
            return new MuleWorkManager(profile, name);
        }
    }

}
