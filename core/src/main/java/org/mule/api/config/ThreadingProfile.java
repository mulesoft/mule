/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.config;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.WorkManager;
import org.mule.config.ChainedThreadingProfile;
import org.mule.config.ImmutableThreadingProfile;
import org.mule.config.pool.ThreadPoolFactory;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.collections.map.CaseInsensitiveMap;

/**
 * <p>
 * Mule uses a few different pools i.e. for service threads and message dispatchers.
 * This interface makes it easier to configure the pool. Pools are created via
 * {@link ImmutableThreadingProfile#createPool(String)} (which should really be a
 * separate factory).
 * </p>
 * <p>
 * {@link ImmutableThreadingProfile} is a simple read-only implementation that makes
 * a local copy of any ThreadingProfile instance passed to a constructor.
 * </p>
 * <p>
 * {@link ChainedThreadingProfile} is a mutable implementation that can take default
 * values from an existing ThreadingProfile. The default values can be either dynamic
 * (read whenever the value is queried) or static (a local copy of the default is
 * made when the profile is first constructed).
 * </p>
 */
public interface ThreadingProfile extends MuleContextAware
{

    /**
     * Default value for MAX_THREADS_ACTIVE
     */
    int DEFAULT_MAX_THREADS_ACTIVE = DefaultThreadingProfileConfig.DEFAULT_MAX_THREADS_ACTIVE;

    /**
     * Default value for MAX_THREADS_IDLE
     */
    int DEFAULT_MAX_THREADS_IDLE = DefaultThreadingProfileConfig.DEFAULT_MAX_THREADS_IDLE;

    /**
     * Default value for MAX_BUFFER_SIZE
     */
    int DEFAULT_MAX_BUFFER_SIZE = DefaultThreadingProfileConfig.DEFAULT_MAX_BUFFER_SIZE;

    /**
     * Default value for MAX_THREAD_TTL
     */
    long DEFAULT_MAX_THREAD_TTL = DefaultThreadingProfileConfig.DEFAULT_MAX_THREAD_TTL;

    /**
     * Default value for DEFAULT_THREAD_WAIT_TIMEOUT
     */
    long DEFAULT_THREAD_WAIT_TIMEOUT = DefaultThreadingProfileConfig.DEFAULT_THREAD_WAIT_TIMEOUT;

    /**
     * Default value for do threading
     */
    boolean DEFAULT_DO_THREADING = true;

    /**
     * Actions to perform on pool exhaustion
     */
    int WHEN_EXHAUSTED_WAIT = 0;
    int WHEN_EXHAUSTED_DISCARD = 1;
    int WHEN_EXHAUSTED_DISCARD_OLDEST = 2;
    int WHEN_EXHAUSTED_ABORT = 3;
    int WHEN_EXHAUSTED_RUN = 4;

    /**
     * Default action to perform on pool exhaustion
     */
    int DEFAULT_POOL_EXHAUST_ACTION = WHEN_EXHAUSTED_RUN;

    // map pool exhaustion strings to their respective values
    Map<?, ?> POOL_EXHAUSTED_ACTIONS = new CaseInsensitiveMap()
    {
        private static final long serialVersionUID = 1L;

        // initializer
        {
            Integer value = WHEN_EXHAUSTED_WAIT;
            this.put("WHEN_EXHAUSTED_WAIT", value);
            this.put("WAIT", value);

            value = WHEN_EXHAUSTED_DISCARD;
            this.put("WHEN_EXHAUSTED_DISCARD", value);
            this.put("DISCARD", value);

            value = WHEN_EXHAUSTED_DISCARD_OLDEST;
            this.put("WHEN_EXHAUSTED_DISCARD_OLDEST", value);
            this.put("DISCARD_OLDEST", value);

            value = WHEN_EXHAUSTED_ABORT;
            this.put("WHEN_EXHAUSTED_ABORT", value);
            this.put("ABORT", value);

            value = WHEN_EXHAUSTED_RUN;
            this.put("WHEN_EXHAUSTED_RUN", value);
            this.put("RUN", value);
        }
    };

    ThreadingProfile DEFAULT_THREADING_PROFILE =
            new ImmutableThreadingProfile(
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

    int getMaxThreadsActive();

    int getMaxThreadsIdle();

    long getThreadTTL();

    long getThreadWaitTimeout();

    int getPoolExhaustedAction();

    RejectedExecutionHandler getRejectedExecutionHandler();

    ThreadFactory getThreadFactory();

    void setMaxThreadsActive(int maxThreadsActive);

    void setMaxThreadsIdle(int maxThreadsIdle);

    void setThreadTTL(long threadTTL);

    void setThreadWaitTimeout(long threadWaitTimeout);

    void setPoolExhaustedAction(int poolExhaustPolicy);

    void setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler);

    void setThreadFactory(ThreadFactory threadFactory);

    int getMaxBufferSize();

    void setMaxBufferSize(int maxBufferSize);

    WorkManagerFactory getWorkManagerFactory();

    void setWorkManagerFactory(WorkManagerFactory workManagerFactory);

    WorkManager createWorkManager(String name, int shutdownTimeout);

    ExecutorService createPool();

    ExecutorService createPool(String name);

    boolean isDoThreading();

    void setDoThreading(boolean doThreading);

    ThreadPoolFactory getPoolFactory();

    ScheduledExecutorService createScheduledPool(String name);

    interface WorkManagerFactory
    {
        WorkManager createWorkManager(ThreadingProfile profile, String name, int shutdownTimeout);
    }

    MuleContext getMuleContext();
}
