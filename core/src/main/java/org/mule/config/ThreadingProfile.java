/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

import org.mule.impl.work.MuleWorkManager;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.util.MapUtils;
import org.mule.util.concurrent.NamedThreadFactory;
import org.mule.util.concurrent.WaitPolicy;

import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingDeque;
import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionHandler;
import edu.emory.mathcs.backport.java.util.concurrent.SynchronousQueue;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadFactory;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import java.util.Map;

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
    public static final int DEFAULT_MAX_THREADS_ACTIVE = 8;

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
    private static final Map POOL_EXHAUSTED_ACTIONS = new CaseInsensitiveMap()
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

    private int maxThreadsActive = DEFAULT_MAX_THREADS_ACTIVE;
    private int maxThreadsIdle = DEFAULT_MAX_THREADS_IDLE;
    private int maxBufferSize = DEFAULT_MAX_BUFFER_SIZE;
    private long threadTTL = DEFAULT_MAX_THREAD_TTL;
    private long threadWaitTimeout = DEFAULT_THREAD_WAIT_TIMEOUT;
    private int poolExhaustPolicy = DEFAULT_POOL_EXHAUST_ACTION;
    private boolean doThreading = DEFAULT_DO_THREADING;

    private WorkManagerFactory workManagerFactory = new DefaultWorkManagerFactory();

    private RejectedExecutionHandler rejectedExecutionHandler;

    private ThreadFactory threadFactory;

    public ThreadingProfile()
    {
        super();
    }

    public ThreadingProfile(int maxThreadsActive,
                            int maxThreadsIdle,
                            long threadTTL,
                            int poolExhaustPolicy,
                            RejectedExecutionHandler rejectedExecutionHandler,
                            ThreadFactory threadFactory)
    {
        this.maxThreadsActive = maxThreadsActive;
        this.maxThreadsIdle = maxThreadsIdle;
        this.threadTTL = threadTTL;
        this.poolExhaustPolicy = poolExhaustPolicy;
        this.rejectedExecutionHandler = rejectedExecutionHandler;
        this.threadFactory = threadFactory;
    }

    public ThreadingProfile(ThreadingProfile tp)
    {
        this.maxThreadsActive = tp.getMaxThreadsActive();
        this.maxThreadsIdle = tp.getMaxThreadsIdle();
        this.maxBufferSize = tp.getMaxBufferSize();
        this.threadTTL = tp.getThreadTTL();
        this.threadWaitTimeout = tp.getThreadWaitTimeout();
        this.poolExhaustPolicy = tp.getPoolExhaustedAction();
        this.doThreading = tp.isDoThreading();
        this.rejectedExecutionHandler = tp.getRejectedExecutionHandler();
        this.threadFactory = tp.getThreadFactory();
        this.workManagerFactory = tp.getWorkManagerFactory();
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
        return poolExhaustPolicy;
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
        this.poolExhaustPolicy = poolExhaustPolicy;
    }

    public void setPoolExhaustedActionString(String poolExhaustPolicy)
    {
        this.poolExhaustPolicy = MapUtils.getIntValue(POOL_EXHAUSTED_ACTIONS, poolExhaustPolicy,
            DEFAULT_POOL_EXHAUST_ACTION);
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

        if (maxBufferSize > 0 && maxThreadsActive > 1)
        {
            buffer = new LinkedBlockingDeque(maxBufferSize);
        }
        else
        {
            buffer = new SynchronousQueue();
        }

        if (maxThreadsIdle > maxThreadsActive)
        {
            maxThreadsIdle = maxThreadsActive;
        }

        ThreadPoolExecutor pool = new ThreadPoolExecutor(maxThreadsIdle, maxThreadsActive, threadTTL,
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
            switch (poolExhaustPolicy)
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
                    pool.setRejectedExecutionHandler(new WaitPolicy(threadWaitTimeout, TimeUnit.MILLISECONDS));
                    break;
            }
        }

        return pool;
    }

    public boolean isDoThreading()
    {
        return doThreading;
    }

    public void setDoThreading(boolean doThreading)
    {
        this.doThreading = doThreading;
    }

    public String toString()
    {
        return "ThreadingProfile{" + "maxThreadsActive=" + maxThreadsActive + ", maxThreadsIdle="
                        + maxThreadsIdle + ", maxBufferSize=" + maxBufferSize + ", threadTTL=" + threadTTL
                        + ", poolExhaustPolicy=" + poolExhaustPolicy + ", threadWaitTimeout="
                        + threadWaitTimeout + ", doThreading=" + doThreading + ", workManagerFactory="
                        + workManagerFactory + ", rejectedExecutionHandler=" + rejectedExecutionHandler
                        + ", threadFactory=" + threadFactory + "}";
    }

    public static interface WorkManagerFactory
    {
        UMOWorkManager createWorkManager(ThreadingProfile profile, String name);
    }

    private class DefaultWorkManagerFactory implements WorkManagerFactory
    {
        public UMOWorkManager createWorkManager(ThreadingProfile profile, String name)
        {
            return new MuleWorkManager(profile, name);
        }
    }

}
