/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.config;

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.ThreadFactory;
import org.mule.impl.work.MuleWorkManager;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.util.DisposableThreadPool;

/**
 * <code>ThreadingProfile</code> is used to configure a thread pool. Mule uses a few
 * different pools i.e. for component threds and message dispatchers. This object makes
 * it easier to configure the pool.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ThreadingProfile
{

    public static final int WHEN_EXHAUSTED_WAIT = 0;
    public static final int WHEN_EXHAUSTED_DISCARD = 1;
    public static final int WHEN_EXHAUSTED_DISCARD_OLDEST = 2;
    public static final int WHEN_EXHAUSTED_ABORT = 3;
    public static final int WHEN_EXHAUSTED_RUN = 4;
    /**
     * Default value for MAX_THREADS_ACTIVE
     */
    public static final int DEFAULT_MAX_THREADS_ACTIVE = 10;
    /**
     * Default value for MAX_THREADS_IDLE
     */
    public static final int DEFAULT_MAX_THREADS_IDLE = 10;
    /**
     * Default value for MAX_BUFFER_SIZE
     */
    public static final int DEFAULT_MAX_BUFFER_SIZE = 0;
    /**
     * Default value for MAX_THREAD_TTL
     */
    public static final long DEFAULT_MAX_THREAD_TTL = 60000;

    /**
     * Default value for do threading
     */
    public static final boolean DEFAULT_DO_THREADING = true;
    /**
     * Default value for POOL_INITIALISATION_POLICY
     */
    public static final int DEFAULT_POOL_EXHAUST_ACTION = WHEN_EXHAUSTED_RUN;

    private int maxThreadsActive = DEFAULT_MAX_THREADS_ACTIVE;
    private int maxThreadsIdle = DEFAULT_MAX_THREADS_IDLE;
    private int maxBufferSize = DEFAULT_MAX_BUFFER_SIZE;
    private long threadTTL = DEFAULT_MAX_THREAD_TTL;
    private int poolExhaustPolicy = DEFAULT_POOL_EXHAUST_ACTION;
    private boolean doThreading = DEFAULT_DO_THREADING;
    private int threadPriority = Thread.NORM_PRIORITY;

    private WorkManagerFactory workManagerFactory = new DefaultWorkManagerFactory();

    private PooledExecutor.BlockedExecutionHandler blockedExecutionHandler;

    private ThreadFactory threadFactory;

    public ThreadingProfile()
    {
    }

    public ThreadingProfile(int maxThreadsActive, int maxThreadsIdle, long threadTTL, int poolExhaustPolicy,
                            PooledExecutor.BlockedExecutionHandler blockedExecutionHandler,
                            ThreadFactory threadFactory)
    {
        this.maxThreadsActive = maxThreadsActive;
        this.maxThreadsIdle = maxThreadsIdle;
        this.threadTTL = threadTTL;
        this.poolExhaustPolicy = poolExhaustPolicy;
        this.blockedExecutionHandler = blockedExecutionHandler;
        this.threadFactory = threadFactory;
    }

    public ThreadingProfile(ThreadingProfile tp)
    {
        this.maxThreadsActive = tp.getMaxThreadsActive();
        this.maxThreadsIdle = tp.getMaxThreadsIdle();
        this.threadTTL = tp.getThreadTTL();
        this.poolExhaustPolicy = tp.getPoolExhaustedAction();
        this.blockedExecutionHandler = tp.getBlockedExecutionHandler();
        this.threadFactory = tp.getThreadFactory();
        this.workManagerFactory = tp.getWorkManagerFactory();
        this.threadPriority = tp.getThreadPriority();

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

    public int getThreadPriority() {
        return threadPriority;
    }

    public void setThreadPriority(int threadPriority) {
        this.threadPriority = threadPriority;
    }

    public int getPoolExhaustedAction()
    {
        return poolExhaustPolicy;
    }

    public PooledExecutor.BlockedExecutionHandler getBlockedExecutionHandler()
    {
        return blockedExecutionHandler;
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

    public void setPoolExhaustedAction(int poolExhaustPolicy)
    {
        this.poolExhaustPolicy = poolExhaustPolicy;
    }

    public void setPoolExhaustedActionString(String poolExhaustPolicy)
    {
        if (poolExhaustPolicy != null)
        {
            if ("WAIT".equals(poolExhaustPolicy))
            {
                this.poolExhaustPolicy = WHEN_EXHAUSTED_WAIT;
            } else if ("ABORT".equals(poolExhaustPolicy))
            {
                this.poolExhaustPolicy = WHEN_EXHAUSTED_ABORT;
            } else if ("DISCARD".equals(poolExhaustPolicy))
            {
                this.poolExhaustPolicy = WHEN_EXHAUSTED_DISCARD;
            } else if ("DISCARD_OLDEST".equals(poolExhaustPolicy))
            {
                this.poolExhaustPolicy = WHEN_EXHAUSTED_DISCARD_OLDEST;
            } else
            {
                this.poolExhaustPolicy = WHEN_EXHAUSTED_RUN;
            }
        }
    }

    public void setBlockedExecutionHandler(PooledExecutor.BlockedExecutionHandler blockedExecutionHandler)
    {
        this.blockedExecutionHandler = blockedExecutionHandler;
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

    public WorkManagerFactory getWorkManagerFactory() {
        return workManagerFactory;
    }

    public void setWorkManagerFactory(WorkManagerFactory workManagerFactory) {
        this.workManagerFactory = workManagerFactory;
    }

    public UMOWorkManager createWorkManager(String name) {
        return workManagerFactory.createWorkManager(this, name);
    }

    public PooledExecutor createPool()
    {
        return configurePool();
    }

    public PooledExecutor createPool(String name)
    {
        threadFactory = new NamedThreadFactory(name, threadPriority);
        return configurePool();
    }

    public void configurePool(PooledExecutor pool)
    {
        pool.setMinimumPoolSize(maxThreadsIdle);
        pool.setMaximumPoolSize(maxThreadsActive);
        pool.setKeepAliveTime(threadTTL);
        if (blockedExecutionHandler != null) pool.setBlockedExecutionHandler(blockedExecutionHandler);
        if (threadFactory != null) pool.setThreadFactory(threadFactory);

        switch (poolExhaustPolicy)
        {
            case WHEN_EXHAUSTED_DISCARD_OLDEST:
                {
                    pool.discardOldestWhenBlocked();
                    break;
                }
            case WHEN_EXHAUSTED_RUN:
                {
                    pool.runWhenBlocked();
                    break;
                }
            case WHEN_EXHAUSTED_ABORT:
                {
                    pool.abortWhenBlocked();
                    break;
                }
            case WHEN_EXHAUSTED_DISCARD:
                {
                    pool.discardWhenBlocked();
                    break;
                }
            case WHEN_EXHAUSTED_WAIT:
                {
                    pool.waitWhenBlocked();
                    break;
                }
            default:
                {
                    pool.waitWhenBlocked();
                    break;
                }
        }
    }

    private PooledExecutor configurePool()
    {
        PooledExecutor pool;
        if (maxBufferSize > 0)
        {
            pool = new DisposableThreadPool(new BoundedBuffer(maxBufferSize));
        } else
        {
            pool = new DisposableThreadPool();
        }
        configurePool(pool);
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

    public String toString() {
        return "ThreadingProfile{" +
                "maxThreadsActive=" + maxThreadsActive +
                ", maxThreadsIdle=" + maxThreadsIdle +
                ", maxBufferSize=" + maxBufferSize +
                ", threadTTL=" + threadTTL +
                ", poolExhaustPolicy=" + poolExhaustPolicy +
                ", doThreading=" + doThreading +
                ", threadPriority=" + threadPriority +
                ", workManagerFactory=" + workManagerFactory +
                ", blockedExecutionHandler=" + blockedExecutionHandler +
                ", threadFactory=" + threadFactory +
                "}";
    }

    public static class NamedThreadFactory implements ThreadFactory
    {
        private String name;
        private int priority;
        private int counter = 1;

        public NamedThreadFactory(String name, int priority)
        {
            this.name = name;
            this.priority = priority;
        }

        public Thread newThread(Runnable runnable)
        {
            Thread t = new Thread(runnable, name + "." + counter++);
            t.setPriority(priority);
            return t;
        }
    }

    public static interface WorkManagerFactory {
        public UMOWorkManager createWorkManager(ThreadingProfile profile, String name);
    }

    private class DefaultWorkManagerFactory implements WorkManagerFactory
    {
        public UMOWorkManager createWorkManager(ThreadingProfile profile, String name) {
            return new MuleWorkManager(profile, name);
        }
    }
}
