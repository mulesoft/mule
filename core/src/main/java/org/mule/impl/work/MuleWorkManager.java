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

import edu.emory.mathcs.backport.java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.ThreadingProfile;
import org.mule.umo.UMOException;
import org.mule.umo.manager.UMOWorkManager;

import javax.resource.spi.XATerminator;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;

/**
 * <code>MuleWorkManager</code> is a Jca Work manager implementation used to
 * manage thread allocation for Mule components and connectors
 * 
 * This code has been adapted from the Geronimo implementation, and the
 * different pool executors have be used directly from Geronimo
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleWorkManager implements UMOWorkManager
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(MuleWorkManager.class);

    /**
     * Pool of threads used by this MuleWorkManager in order to process the Work
     * instances submitted via the doWork methods.
     */
    private WorkExecutorPool syncWorkExecutorPool;

    /**
     * Pool of threads used by this MuleWorkManager in order to process the Work
     * instances submitted via the startWork methods.
     */
    private WorkExecutorPool startWorkExecutorPool;

    /**
     * Pool of threads used by this MuleWorkManager in order to process the Work
     * instances submitted via the scheduleWork methods.
     */
    private WorkExecutorPool scheduledWorkExecutorPool;

    private final WorkExecutor scheduleWorkExecutor = new ScheduleWorkExecutor();
    private final WorkExecutor startWorkExecutor = new StartWorkExecutor();
    private final WorkExecutor syncWorkExecutor = new SyncWorkExecutor();

    /**
     * Create a MuleWorkManager.
     */
    public MuleWorkManager()
    {
        this(MuleManager.getConfiguration().getDefaultThreadingProfile(), null);
    }

    public MuleWorkManager(ThreadingProfile profile, String name)
    {
        if (name == null) {
            name = "WorkManager#" + hashCode();
        }
        syncWorkExecutorPool = new NullWorkExecutorPool(profile, name);
        startWorkExecutorPool = new NullWorkExecutorPool(profile, name);
        scheduledWorkExecutorPool = new NullWorkExecutorPool(profile, name);
    }

    public void start() throws UMOException
    {
        syncWorkExecutorPool = syncWorkExecutorPool.start();
        startWorkExecutorPool = startWorkExecutorPool.start();
        scheduledWorkExecutorPool = scheduledWorkExecutorPool.start();
    }

    public void stop() throws UMOException
    {
        syncWorkExecutorPool = syncWorkExecutorPool.stop();
        startWorkExecutorPool = startWorkExecutorPool.stop();
        scheduledWorkExecutorPool = scheduledWorkExecutorPool.stop();
    }

    public void dispose()
    {
        try {
            stop();
        } catch (UMOException e) {
            logger.warn("Error while disposing Work Manager: " + e.getMessage(), e);

        }
    }

    // TODO
    public XATerminator getXATerminator()
    {
        return null;
    }

    public int getSyncThreadCount()
    {
        return syncWorkExecutorPool.getPoolSize();
    }

    public int getSyncMaximumPoolSize()
    {
        return syncWorkExecutorPool.getMaximumPoolSize();
    }

    public void setSyncMaximumPoolSize(int maxSize)
    {
        syncWorkExecutorPool.setMaximumPoolSize(maxSize);
    }

    public int getStartThreadCount()
    {
        return startWorkExecutorPool.getPoolSize();
    }

    public int getStartMaximumPoolSize()
    {
        return startWorkExecutorPool.getMaximumPoolSize();
    }

    public void setStartMaximumPoolSize(int maxSize)
    {
        startWorkExecutorPool.setMaximumPoolSize(maxSize);
    }

    public int getScheduledThreadCount()
    {
        return scheduledWorkExecutorPool.getPoolSize();
    }

    public int getScheduledMaximumPoolSize()
    {
        return scheduledWorkExecutorPool.getMaximumPoolSize();
    }

    public void setScheduledMaximumPoolSize(int maxSize)
    {
        scheduledWorkExecutorPool.setMaximumPoolSize(maxSize);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.resource.spi.work.MuleWorkManager#doWork(javax.resource.spi.work.Work)
     */
    public void doWork(Work work) throws WorkException
    {
        executeWork(new WorkerContext(work), syncWorkExecutor, syncWorkExecutorPool);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.resource.spi.work.MuleWorkManager#doWork(javax.resource.spi.work.Work,
     *      long, javax.resource.spi.work.ExecutionContext,
     *      javax.resource.spi.work.WorkListener)
     */
    public void doWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener)
            throws WorkException
    {
        WorkerContext workWrapper = new WorkerContext(work, startTimeout, execContext, workListener);
        workWrapper.setThreadPriority(Thread.currentThread().getPriority());
        executeWork(workWrapper, syncWorkExecutor, syncWorkExecutorPool);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.resource.spi.work.MuleWorkManager#startWork(javax.resource.spi.work.Work)
     */
    public long startWork(Work work) throws WorkException
    {
        WorkerContext workWrapper = new WorkerContext(work);
        workWrapper.setThreadPriority(Thread.currentThread().getPriority());
        executeWork(workWrapper, startWorkExecutor, startWorkExecutorPool);
        return System.currentTimeMillis() - workWrapper.getAcceptedTime();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.resource.spi.work.MuleWorkManager#startWork(javax.resource.spi.work.Work,
     *      long, javax.resource.spi.work.ExecutionContext,
     *      javax.resource.spi.work.WorkListener)
     */
    public long startWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener)
            throws WorkException
    {
        WorkerContext workWrapper = new WorkerContext(work, startTimeout, execContext, workListener);
        workWrapper.setThreadPriority(Thread.currentThread().getPriority());
        executeWork(workWrapper, startWorkExecutor, startWorkExecutorPool);
        return System.currentTimeMillis() - workWrapper.getAcceptedTime();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.resource.spi.work.MuleWorkManager#scheduleWork(javax.resource.spi.work.Work)
     */
    public void scheduleWork(Work work) throws WorkException
    {
        WorkerContext workWrapper = new WorkerContext(work);
        workWrapper.setThreadPriority(Thread.currentThread().getPriority());
        executeWork(workWrapper, scheduleWorkExecutor, scheduledWorkExecutorPool);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.resource.spi.work.MuleWorkManager#scheduleWork(javax.resource.spi.work.Work,
     *      long, javax.resource.spi.work.ExecutionContext,
     *      javax.resource.spi.work.WorkListener)
     */
    public void scheduleWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener)
            throws WorkException
    {
        WorkerContext workWrapper = new WorkerContext(work, startTimeout, execContext, workListener);
        workWrapper.setThreadPriority(Thread.currentThread().getPriority());
        executeWork(workWrapper, scheduleWorkExecutor, scheduledWorkExecutorPool);
    }

    /**
     * Execute the specified Work.
     * 
     * @param work Work to be executed.
     * 
     * @exception WorkException Indicates that the Work execution has been
     *                unsuccessful.
     */
    private void executeWork(WorkerContext work, WorkExecutor workExecutor, Executor pooledExecutor)
            throws WorkException
    {
        work.workAccepted(this);
        try {
            workExecutor.doExecute(work, pooledExecutor);
            WorkException exception = work.getWorkException();
            if (null != exception) {
                throw exception;
            }
        } catch (InterruptedException e) {
            WorkCompletedException wcj = new WorkCompletedException("The execution has been interrupted.", e);
            wcj.setErrorCode(WorkException.INTERNAL);
            throw wcj;
        }
    }
}
