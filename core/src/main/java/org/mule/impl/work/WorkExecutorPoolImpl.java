/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 *
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.mule.impl.work;

import org.mule.config.ThreadingProfile;

import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for WorkExecutorPool. Sub-classes define the synchronization policy
 * (should the call block until the end of the work or when it starts et cetera).
 */
public class WorkExecutorPoolImpl implements WorkExecutorPool
{
    private static final long SHUTDOWN_TIMEOUT = 5000L;

    private final Log logger = LogFactory.getLog(getClass());

    private final ThreadPoolExecutor pooledExecutor;
    private final ThreadingProfile profile;
    private final String name;

    /**
     * Creates a pool with the specified minimum and maximum sizes. The Channel used
     * to enqueue the submitted Work instances is queueless synchronous one.
     * 
     * @param profile The threading profile to use when creating a pool for this work
     *            manager
     * @param name The name to associate with the threads created in this pool
     */
    public WorkExecutorPoolImpl(ThreadingProfile profile, String name)
    {
        this.profile = profile;
        this.name = name;
        this.pooledExecutor = profile.createPool(name);
    }

    /**
     * Execute the specified Work.
     * 
     * @param work Work to be executed.
     * @exception InterruptedException Indicates that the Work execution has been
     *                unsuccessful.
     */
    public void execute(Runnable work)
    {
        pooledExecutor.execute(work);
    }

    /**
     * Gets the size of this pool.
     */
    public int getPoolSize()
    {
        return pooledExecutor.getPoolSize();
    }

    /**
     * Gets the maximum size of this pool.
     */
    public int getMaximumPoolSize()
    {
        return pooledExecutor.getMaximumPoolSize();
    }

    /**
     * Sets the maximum size of this pool.
     * 
     * @param maxSize New maximum size of this pool.
     */
    public void setMaximumPoolSize(int maxSize)
    {
        pooledExecutor.setMaximumPoolSize(maxSize);
    }

    public WorkExecutorPool start()
    {
        throw new IllegalStateException("This WorkExecutorPool is already started");
    }

    /**
     * Stops this pool immediately: all threads are interrupted and waited for.
     */
    public WorkExecutorPool stop()
    {
        // Cancel currently executing tasks
        List outstanding = pooledExecutor.shutdownNow();

        try
        {
            // Wait a while for existing tasks to terminate
            if (!pooledExecutor.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS))
            {
                logger.warn("Pool " + name + " did not terminate in time; " + outstanding.size()
                                + " work items were cancelled.");
            }
        }
        catch (InterruptedException ie)
        {
            // (Re-)Cancel if current thread also interrupted
            pooledExecutor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }

        return new NullWorkExecutorPool(profile, name);
    }

}
