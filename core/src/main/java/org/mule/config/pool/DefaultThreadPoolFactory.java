/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.pool;

import org.mule.api.MuleContext;
import org.mule.api.config.ThreadingProfile;
import org.mule.util.StringUtils;
import org.mule.util.concurrent.NamedThreadFactory;
import org.mule.util.concurrent.WaitPolicy;

import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingDeque;
import edu.emory.mathcs.backport.java.util.concurrent.SynchronousQueue;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
*
*/
public class DefaultThreadPoolFactory extends ThreadPoolFactory
{
    // deliberately shadow the superclass' static logger as to avoid log congestion on it
    protected final Log logger = LogFactory.getLog(getClass());

    public ThreadPoolExecutor createPool(String name, ThreadingProfile tp)
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

        ThreadPoolExecutor pool = internalCreatePool(name, tp, buffer);
        configureThreadFactory(name, tp, pool);


        if (tp.getRejectedExecutionHandler() != null)
        {
            pool.setRejectedExecutionHandler(tp.getRejectedExecutionHandler());
        }
        else
        {
            switch (tp.getPoolExhaustedAction())
            {
                case ThreadingProfile.WHEN_EXHAUSTED_DISCARD_OLDEST :
                    pool.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
                    break;
                case ThreadingProfile.WHEN_EXHAUSTED_RUN :
                    pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
                    break;
                case ThreadingProfile.WHEN_EXHAUSTED_ABORT :
                    pool.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
                    break;
                case ThreadingProfile.WHEN_EXHAUSTED_DISCARD :
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

    protected void configureThreadFactory(String name, ThreadingProfile tp, ThreadPoolExecutor pool)
    {
        // use a custom ThreadFactory if one has been configured
        if (tp.getThreadFactory() != null)
        {
            pool.setThreadFactory(tp.getThreadFactory());
        }
        else
        {
            // ..else create a "NamedThreadFactory" if a proper name was passed in
            if (StringUtils.isNotBlank(name))
            {
                // Use MuleContext classloader so that other temporary classloaders
                // aren't used when things are started lazily or from elsewhere.
                pool.setThreadFactory(new NamedThreadFactory(name, MuleContext.class.getClassLoader()));
            }
            else
            {
                // let ThreadPoolExecutor create a default ThreadFactory;
                // see Executors.defaultThreadFactory()
            }
        }
    }

    protected ThreadPoolExecutor internalCreatePool(String name, ThreadingProfile tp, BlockingQueue buffer)
    {
        return new ThreadPoolExecutor(Math.min(tp.getMaxThreadsIdle(), tp.getMaxThreadsActive()),
                                      tp.getMaxThreadsActive(), tp.getThreadTTL(),
                                      TimeUnit.MILLISECONDS, buffer);

    }
}
