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
import org.mule.config.pool.DefaultThreadPoolFactory;
import org.mule.config.pool.ThreadPoolFactory;
import org.mule.util.concurrent.NamedThreadFactory;

import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * {@link org.mule.api.config.ThreadingProfile} implementation that executes work in the caller thread.
 */
public class DirectThreadingProfile extends ImmutableThreadingProfile
{

    public DirectThreadingProfile()
    {
        super(0, 0, 0, 0, 0, 0, false, new ThreadPoolExecutor.CallerRunsPolicy(), new NullThreadFactory());
    }

    @Override
    public ExecutorService createPool(String name)
    {
        return MoreExecutors.newDirectExecutorService();
    }

    @Override
    public ScheduledExecutorService createScheduledPool(String name)
    {
        throw new UnsupportedOperationException("This implementation does not support the creation of scheduled pools");
    }

    private static class NullThreadFactory implements ThreadFactory
    {

        @Override
        public Thread newThread(Runnable r)
        {
            throw new UnsupportedOperationException();
        }
    }
}
