/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.reconnect;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.WorkManager;
import org.mule.util.concurrent.ThreadNameHelper;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;

/**
 * Fake work manager for executing jms endpoint level reconnection thread.
 *
 * It will create only one thread since it's bound to one endpoint and it only requires one thread.
 */
public class ReconnectWorkManager implements WorkManager
{

    private final WorkDelegate workDelegate;
    private Thread reconnectThread;
    private boolean isStarted = false;
    private MuleContext muleContext;

    public ReconnectWorkManager(MuleContext muleContext)
    {
        this.muleContext = muleContext;
        this.workDelegate = new WorkDelegate();
        this.reconnectThread = new Thread(workDelegate, String.format("%s.endpoint.reconnection", ThreadNameHelper.getPrefix(muleContext)));
    }

    @Override
    public boolean isStarted()
    {
        return isStarted;
    }

    @Override
    public void dispose()
    {
        stop();
    }

    public void stop()
    {
        reconnectThread.interrupt();
        isStarted = false;
    }

    @Override
    public void execute(Runnable runnable)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void start() throws MuleException
    {
        isStarted = true;
    }

    @Override
    public void doWork(Work work) throws WorkException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void doWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener) throws WorkException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public long startWork(Work work) throws WorkException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public long startWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener) throws WorkException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void scheduleWork(Work work) throws WorkException
    {
        this.workDelegate.setWork(work);
        this.reconnectThread.start();
    }

    @Override
    public void scheduleWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener) throws WorkException
    {
        throw new UnsupportedOperationException();
    }

    public static class WorkDelegate implements Work
    {
        private Work work;

        public void setWork(Work work)
        {
            this.work = work;
        }

        @Override
        public void release()
        {
            work.release();
        }

        @Override
        public void run()
        {
            work.run();
        }
    }
}