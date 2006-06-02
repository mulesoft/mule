/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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

import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import org.mule.config.ThreadingProfile;
import org.mule.util.concurrent.WaitPolicy;

/**
 * Based class for WorkExecutorPool. Sub-classes define the synchronization
 * policy (should the call block until the end of the work; or when it starts et
 * cetera).
 * 
 * @version $Rev$ $Date$
 */
public class WorkExecutorPoolImpl implements WorkExecutorPool
{

    /**
     * A timed out pooled executor.
     */
    private ThreadPoolExecutor pooledExecutor;

    private ThreadingProfile profile;

    private String name;

    private static final long SHUTDOWN_TIMEOUT = 5000L;

    /**
     * Creates a pool with the specified minimum and maximum sizes. The Channel
     * used to enqueue the submitted Work instances is queueless synchronous
     * one.
     * 
     * @param profile The threading profile to use when creating a pool for this
     *            work manager
     * @param name The name to associate with the threads created in this pool
     */
    public WorkExecutorPoolImpl(ThreadingProfile profile, String name)
    {
        this.profile = profile;
        this.name = name;
        pooledExecutor = profile.createPool(name);
    }

    /**
     * Creates a pool with the specified minimum and maximum sizes and using the
     * specified Channel to enqueue the submitted Work instances.
     * 
     * @param queue Queue to be used as the queueing facility of this pool.
     * @param maxSize Maximum size of the work executor pool.
     */
    public WorkExecutorPoolImpl(BlockingQueue queue, int maxSize)
    {
        pooledExecutor = new ThreadPoolExecutor(0, maxSize, 60L, TimeUnit.SECONDS, queue);
        pooledExecutor.setCorePoolSize(maxSize);
        pooledExecutor.setRejectedExecutionHandler(new WaitPolicy(ThreadingProfile.DEFAULT_THREAD_WAIT_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    /**
     * Execute the specified Work.
     * 
     * @param work Work to be executed.
     * 
     * @exception InterruptedException Indicates that the Work execution has
     *                been unsuccessful.
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
        throw new IllegalStateException("This pooled executor is already started");
    }

    /**
     * Stops this pool. Prior to stop this pool, all the enqueued Work instances
     * are processed, if possible, in the allowed timeout. After what, all
     * threads are interrupted and waited for. This is an mix orderly / abrupt
     * shutdown.
     */
    public WorkExecutorPool stop()
    {
        pooledExecutor.shutdownNow();
        try {
            pooledExecutor.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // Continue
        }
        return new NullWorkExecutorPool(profile, name);
    }

}
