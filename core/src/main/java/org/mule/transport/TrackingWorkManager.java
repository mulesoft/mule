/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.api.MuleException;
import org.mule.api.context.WorkManager;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wraps a {@link WorkManager} to track the work that in process.
 */
public class TrackingWorkManager implements WorkManager
{

    protected static final Log logger = LogFactory.getLog(TrackingWorkManager.class);

    public static final int DEFAULT_SLEEP_MILLIS = 50;
    public static final String MULE_WAIT_MILLIS = "mule.transport.dispose.wait";

    private final WorkManagerHolder delegateHolder;
    private final int shutdownTimeout;
    private final int waitMillis;
    private WorkTracker workTracker;
    private WorkListenerWrapperFactory workListenerWrapperFactory;

    public TrackingWorkManager(WorkManagerHolder workManagerHolder, int shutdownTimeout)
    {
        this.delegateHolder = workManagerHolder;
        this.workTracker = new ConcurrentWorkTracker();
        this.shutdownTimeout = shutdownTimeout;
        this.workListenerWrapperFactory = new TrackerWorkListenerWrapperFactory();
        this.waitMillis = getWaitMillis();
    }

    @Override
    public boolean isStarted()
    {
        return delegateHolder.getWorkManager().isStarted();
    }

    @Override
    public void dispose()
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Waiting for works to finish execution");
        }

        long initialMillis = System.currentTimeMillis();

        while (workTracker.pendingWorks().size() != 0 && !isTimeoutExpired(initialMillis))
        {
            try
            {
                Thread.sleep(waitMillis);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Stop waiting for works completion. There are %s works unfinished works", workTracker.pendingWorks().size()));
        }

        workTracker.dispose();
    }

    @Override
    public void execute(final Runnable runnable)
    {
        try
        {
            workTracker.addWork(runnable);

            delegateHolder.getWorkManager().execute(new TrackeableRunnable(runnable));
        }
        catch (RuntimeException e)
        {
            workTracker.removeWork(runnable);
            throw e;
        }
    }

    @Override
    public void start() throws MuleException
    {
        // Nothing to do
    }

    @Override
    public void doWork(Work work) throws WorkException
    {
        workTracker.addWork(work);

        try
        {
            delegateHolder.getWorkManager().doWork(work);
        }
        finally
        {
            workTracker.removeWork(work);
        }
    }

    @Override
    public void doWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener) throws WorkException
    {
        workTracker.addWork(work);

        try
        {
            delegateHolder.getWorkManager().doWork(work, startTimeout, execContext, workListener);
        }
        finally
        {
            workTracker.removeWork(work);
        }
    }

    @Override
    public long startWork(final Work work) throws WorkException
    {
        try
        {
            workTracker.addWork(work);

            Work wrappedWork = new TrackeableWork(work);

            return delegateHolder.getWorkManager().startWork(wrappedWork);
        }
        catch (WorkException e)
        {
            workTracker.removeWork(work);
            throw e;
        }
        catch (RuntimeException e)
        {
            workTracker.removeWork(work);
            throw e;
        }
    }

    @Override
    public long startWork(final Work work, long startTimeout, ExecutionContext execContext, final WorkListener workListener) throws WorkException
    {
        try
        {
            workTracker.addWork(work);

            TrackeableWork trackeableWork = new TrackeableWork(work);
            return delegateHolder.getWorkManager().startWork(trackeableWork, startTimeout, execContext, workListenerWrapperFactory.create(work, workListener));
        }
        catch (WorkException e)
        {
            workTracker.removeWork(work);
            throw e;
        }
        catch (RuntimeException e)
        {
            workTracker.removeWork(work);
            throw e;
        }
    }

    @Override
    public void scheduleWork(final Work work) throws WorkException
    {
        try
        {
            workTracker.addWork(work);

            Work wrappedWork = new TrackeableWork(work);

            delegateHolder.getWorkManager().scheduleWork(wrappedWork);
        }
        catch (WorkException e)
        {
            workTracker.removeWork(work);
            throw e;
        }
        catch (RuntimeException e)
        {
            workTracker.removeWork(work);
            throw e;
        }
    }

    @Override
    public void scheduleWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener) throws WorkException
    {
        workTracker.addWork(work);

        try
        {
            TrackeableWork trackeableWork = new TrackeableWork(work);

            delegateHolder.getWorkManager().scheduleWork(trackeableWork, startTimeout, execContext, workListenerWrapperFactory.create(work, workListener));
        }
        catch (WorkException e)
        {
            workTracker.removeWork(work);
            throw e;
        }
        catch (RuntimeException e)
        {
            workTracker.removeWork(work);
            throw e;
        }
    }

    private boolean isTimeoutExpired(long initialMillis)
    {
        return System.currentTimeMillis() - initialMillis > shutdownTimeout;
    }

    public void setWorkListenerWrapperFactory(WorkListenerWrapperFactory workListenerWrapperFactory)
    {
        this.workListenerWrapperFactory = workListenerWrapperFactory;
    }

    public void setWorkTracker(WorkTracker workTracker)
    {
        this.workTracker = workTracker;
    }

    private static int getWaitMillis()
    {
        try
        {
            String property = System.getProperty(MULE_WAIT_MILLIS);
            return Integer.parseInt(property);
        }
        catch (NumberFormatException e)
        {
            return DEFAULT_SLEEP_MILLIS;
        }
    }

    private class TrackeableWork implements Work
    {

        private final Work work;

        public TrackeableWork(Work work)
        {
            this.work = work;
        }

        public void run()
        {
            try
            {
                work.run();
            }
            finally
            {
                workTracker.removeWork(work);
            }
        }

        @Override
        public void release()
        {
            work.release();
        }
    }

    private class TrackeableRunnable implements Runnable
    {

        private final Runnable runnable;

        public TrackeableRunnable(Runnable runnable)
        {
            this.runnable = runnable;
        }

        public void run()
        {
            try
            {
                runnable.run();
            }
            finally
            {
                workTracker.removeWork(runnable);
            }
        }
    }
}
