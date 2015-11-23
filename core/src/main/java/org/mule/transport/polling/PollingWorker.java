/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.polling;

import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleException;
import org.mule.api.exception.SystemExceptionHandler;

import javax.resource.spi.work.Work;

/**
 * Runner for a task.
 *
 * The worker is responsible of:
 * <p/>
 * <ul>
 *     <li>Stops the task if the worker get's interrupted</li>
 *     <li>Does not run the task if the worker is stopped</li>
 *     <li>calls {@link SystemExceptionHandler} in case the task throws an exception</li>
 * </ul>
 */
public class PollingWorker implements Work
{
    private final PollingTask task;
    private final SystemExceptionHandler systemExceptionHandler;
    protected volatile boolean running = false;

    /**
     * @param task task to be executed by the worker
     * @param systemExceptionHandler exception handler for task exceptions
     */
    public PollingWorker(PollingTask task, SystemExceptionHandler systemExceptionHandler)
    {
        this.task = task;
        this.systemExceptionHandler = systemExceptionHandler;
    }

    public boolean isRunning()
    {
        return running;
    }

    /**
     * Executes the task. It will exit after each call since it will be invoked again by the scheduler
     */
    @Override
    public void run()
    {
        // Make sure we start with a clean slate.
        RequestContext.clear();
        if (task.isStarted())
        {
            running = true;
            try
            {
                poll();
            }
            catch (InterruptedException e)
            {
                // stop polling
                try
                {
                    task.stop();
                }
                catch (MuleException stopException)
                {
                    systemExceptionHandler.handleException(stopException);
                }
            }
            catch (MessagingException e)
            {
                //Already handled by TransactionTemplate
            }
            catch (Exception e)
            {
                systemExceptionHandler.handleException(e);
            }
            finally
            {
                running = false;
            }
        }
    }

    protected void poll() throws Exception
    {
        task.run();
    }

    @Override
    public void release()
    {
        // nop
    }

}
