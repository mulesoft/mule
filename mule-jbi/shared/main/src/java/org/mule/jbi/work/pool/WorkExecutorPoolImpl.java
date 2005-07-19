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
package org.mule.jbi.work.pool;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.ThreadFactory;

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
    private PooledExecutor pooledExecutor;

    private String name;
    
    //private static final long SHUTDOWN_TIMEOUT = 5000L;

    /**
     * Creates a pool with the specified minimum and maximum sizes. The Channel
     * used to enqueue the submitted Work instances is queueless synchronous
     * one.
     * 
     * @param profile The threading profile to use when creating a pool for this
     *            work manager
     * @param name The name to associate with the threads created in this pool
     */
    public WorkExecutorPoolImpl(int maxSize, String name)
    {
    	this.name = name;
    	this.pooledExecutor = new PooledExecutor(new LinkedQueue(), maxSize);
    	this.pooledExecutor.setThreadFactory(new NamedThreadFactory());
    	this.pooledExecutor.setMinimumPoolSize(maxSize);
    	this.pooledExecutor.waitWhenBlocked();
    }

    /**
     * Execute the specified Work.
     * 
     * @param work Work to be executed.
     * 
     * @exception InterruptedException Indicates that the Work execution has
     *                been unsuccessful.
     */
    public void execute(Runnable work) throws InterruptedException
    {
    	this.pooledExecutor.execute(work);
    }

    /**
     * Gets the size of this pool.
     */
    public int getPoolSize()
    {
        return this.pooledExecutor.getPoolSize();
    }

    /**
     * Gets the maximum size of this pool.
     */
    public int getMaximumPoolSize()
    {
        return this.pooledExecutor.getMaximumPoolSize();
    }

    /**
     * Sets the maximum size of this pool.
     * 
     * @param maxSize New maximum size of this pool.
     */
    public void setMaximumPoolSize(int maxSize)
    {
    	this.pooledExecutor.setMaximumPoolSize(maxSize);
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
        int maxSize = getMaximumPoolSize();
        pooledExecutor.shutdownAfterProcessingCurrentlyQueuedTasks();
        return new NullWorkExecutorPool(maxSize, this.name);
    }

    public class NamedThreadFactory implements ThreadFactory
    {
        private int counter = 1;

        public Thread newThread(Runnable runnable)
        {
            Thread t = new Thread(runnable, WorkExecutorPoolImpl.this.name + "." + counter++);
            return t;
        }
    }

}
