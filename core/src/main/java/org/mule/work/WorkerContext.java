/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.work;

import org.mule.RequestContext;
import org.mule.transaction.TransactionCoordination;
import org.mule.util.concurrent.Latch;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkAdapter;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkRejectedException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>WorkerContext</code> TODO
 */
public class WorkerContext implements Work
{

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(WorkerContext.class);

    /**
     * Null WorkListener used as the default WorkListener.
     */
    private static final WorkListener NULL_WORK_LISTENER = new WorkAdapter()
    {
        @Override
        public void workRejected(WorkEvent event)
        {
            if (event.getException() != null)
            {
                if (event.getException() instanceof WorkCompletedException
                    && event.getException().getCause() != null)
                {
                    logger.error(event.getWork().toString(), event.getException().getCause());
                }
                else
                {
                    logger.error(event.getWork().toString(), event.getException());
                }
            }
        }
    };

    protected ClassLoader executionClassLoader;

    /**
     * Priority of the thread, which will execute this work.
     */
    private int threadPriority;

    /**
     * Actual work to be executed.
     */
    private Work worker;

    /**
     * System.currentTimeMillis() when the wrapped Work has been accepted.
     */
    private long acceptedTime;

    /**
     * Number of times that the execution of this work has been tried.
     */
    private int retryCount;

    /**
     * Time duration (in milliseconds) within which the execution of the Work
     * instance must start.
     */
    private long startTimeOut;

    /**
     * Execution context of the actual work to be executed.
     */
    private final ExecutionContext executionContext;

    /**
     * Listener to be notified during the life-cycle of the work treatment.
     */
    private WorkListener workListener = NULL_WORK_LISTENER;

    /**
     * Work exception, if any.
     */
    private WorkException workException;

    /**
     * A latch, which is released when the work is started.
     */
    private final Latch startLatch = new Latch();

    /**
     * A latch, which is released when the work is completed.
     */
    private final Latch endLatch = new Latch();

    {
        this.executionClassLoader = Thread.currentThread().getContextClassLoader();
    }

    /**
     * Create a WorkWrapper.
     * 
     * @param work Work to be wrapped.
     */
    public WorkerContext(Work work)
    {
        worker = work;
        executionContext = null;
    }

    /**
     * Create a WorkWrapper with the specified execution context.
     * 
     * @param aWork Work to be wrapped.
     * @param aStartTimeout a time duration (in milliseconds) within which the
     *            execution of the Work instance must start.
     * @param execContext an object containing the execution context with which the
     *            submitted Work instance must be executed.
     * @param workListener an object which would be notified when the various Work
     *            processing events (work accepted, work rejected, work started,
     */
    public WorkerContext(Work aWork,
                         long aStartTimeout,
                         ExecutionContext execContext,
                         WorkListener workListener)
    {
        worker = aWork;
        startTimeOut = aStartTimeout;
        executionContext = execContext;
        if (null != workListener)
        {
            this.workListener = workListener;
        }
    }

    public void release()
    {
        worker.release();
    }

    /**
     * Defines the thread priority level of the thread, which will be dispatched to
     * process this work. This priority level must be the same one for a given
     * resource adapter.
     * 
     * @param aPriority Priority of the thread to be used to process the wrapped Work
     *            instance.
     */
    public void setThreadPriority(int aPriority)
    {
        threadPriority = aPriority;
    }

    /**
     * Gets the priority level of the thread, which will be dispatched to process
     * this work. This priority level must be the same one for a given resource
     * adapter.
     * 
     * @return The priority level of the thread to be dispatched to process the
     *         wrapped Work instance.
     */
    public int getThreadPriority()
    {
        return threadPriority;
    }

    /**
     * Call-back method used by a Work executor in order to notify this instance that
     * the wrapped Work instance has been accepted.
     * 
     * @param anObject Object on which the event initially occurred. It should be the
     *            work executor.
     */
    public synchronized void workAccepted(Object anObject)
    {
        acceptedTime = System.currentTimeMillis();
        workListener.workAccepted(new WorkEvent(anObject, WorkEvent.WORK_ACCEPTED, worker, null));
    }

    /**
     * System.currentTimeMillis() when the Work has been accepted. This method can be
     * used to compute the duration of a work.
     * 
     * @return When the work has been accepted.
     */
    public synchronized long getAcceptedTime()
    {
        return acceptedTime;
    }

    /**
     * Gets the time duration (in milliseconds) within which the execution of the
     * Work instance must start.
     * 
     * @return Time out duration.
     */
    public long getStartTimeout()
    {
        return startTimeOut;
    }

    /**
     * Used by a Work executor in order to know if this work, which should be
     * accepted but not started has timed out. This method MUST be called prior to
     * retry the execution of a Work.
     * 
     * @return true if the Work has timed out and false otherwise.
     */
    public synchronized boolean isTimedOut()
    {
        // A value of 0 means that the work never times out.
        // ??? really?
        if (0 == startTimeOut || startTimeOut == MuleWorkManager.INDEFINITE)
        {
            return false;
        }
        boolean isTimeout = acceptedTime + startTimeOut > 0
                            && System.currentTimeMillis() > acceptedTime + startTimeOut;
        if (logger.isDebugEnabled())
        {
            logger.debug(this + " accepted at " + acceptedTime
                         + (isTimeout ? " has timed out." : " has not timed out. ") + retryCount
                         + " retries have been performed.");
        }
        if (isTimeout)
        {
            workException = new WorkRejectedException(this + " has timed out.", WorkException.START_TIMED_OUT);
            workListener.workRejected(new WorkEvent(this, WorkEvent.WORK_REJECTED, worker, workException));
            return true;
        }
        retryCount++;
        return isTimeout;
    }

    /**
     * Gets the WorkException, if any, thrown during the execution.
     * 
     * @return WorkException, if any.
     */
    public synchronized WorkException getWorkException()
    {
        return workException;
    }

    public void run()
    {
        if (isTimedOut())
        {
            // In case of a time out, one releases the start and end latches
            // to prevent a dead-lock.
            startLatch.countDown();
            endLatch.countDown();
            return;
        }
        // Implementation note: the work listener is notified prior to release
        // the start lock. This behavior is intentional and seems to be the
        // more conservative.
        workListener.workStarted(new WorkEvent(this, WorkEvent.WORK_STARTED, worker, null));
        startLatch.countDown();
        // Implementation note: we assume this is being called without an
        // interesting TransactionContext,
        // and ignore/replace whatever is associated with the current thread.
        try
        {
            final ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
            try
            {
                // execute with the application or domain specific classloader in the context
                Thread.currentThread().setContextClassLoader(executionClassLoader);
                worker.run();
            }
            finally
            {
                Thread.currentThread().setContextClassLoader(originalCl);
            }
            workListener.workCompleted(new WorkEvent(this, WorkEvent.WORK_COMPLETED, worker, null));
        }
        catch (Throwable e)
        {
            workException = (WorkException)(e instanceof WorkCompletedException
                            ? e : new WorkCompletedException("Unknown error",
                                WorkCompletedException.UNDEFINED).initCause(e));
            workListener.workCompleted(new WorkEvent(this, WorkEvent.WORK_REJECTED, worker, workException));
        }
        finally
        {
            RequestContext.clear();
            TransactionCoordination.getInstance().clear();
            endLatch.countDown();
        }
    }

    /**
     * Provides a latch, which can be used to wait the start of a work execution.
     * 
     * @return Latch that a caller can acquire to wait for the start of a work
     *         execution.
     */
    public Latch provideStartLatch()
    {
        return startLatch;
    }

    /**
     * Provides a latch, which can be used to wait the end of a work execution.
     * 
     * @return Latch that a caller can acquire to wait for the end of a work
     *         execution.
     */
    public Latch provideEndLatch()
    {
        return endLatch;
    }

    @Override
    public String toString()
    {
        return "Work: " + worker;
    }
}
