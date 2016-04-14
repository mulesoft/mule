/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.work;

import org.mule.api.MuleException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManager;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;

/**
 * A simple implementation of {@link WorkManager} which
 * executes tasks on the invoking thread. As a result, every operation
 * on this class is blocking.
 * <p/>
 * This class is useful in components configured through a
 * {@link ThreadingProfile} in which {@link ThreadingProfile#isDoThreading()}
 * is {@code false}.
 *
 * @since 3.6.0
 */
public class SerialWorkManager implements WorkManager
{

    @Override
    public void execute(Runnable command)
    {
        command.run();
    }

    @Override
    public void doWork(Work work) throws WorkException
    {
        work.run();
    }

    @Override
    public void doWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener) throws WorkException
    {
        doWork(work);
    }

    @Override
    public long startWork(Work work) throws WorkException
    {
        doWork(work);
        return 0;
    }

    @Override
    public long startWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener) throws WorkException
    {
        doWork(work);
        return 0;
    }

    @Override
    public void scheduleWork(Work work) throws WorkException
    {
        doWork(work);
    }

    @Override
    public void scheduleWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener) throws WorkException
    {
        doWork(work);
    }

    @Override
    public boolean isStarted()
    {
        return true;
    }

    @Override
    public void start() throws MuleException
    {
    }

    @Override
    public void dispose()
    {
    }
}
