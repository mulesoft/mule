/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

import org.mule.runtime.core.api.context.WorkManager;

public class PollingReceiverWorkerSchedule implements Runnable
{

    protected final PollingReceiverWorker worker;
    protected final WorkManager workManager;
    protected final AbstractPollingMessageReceiver receiver;
    private final ClassLoader classLoader;

    protected PollingReceiverWorkerSchedule(PollingReceiverWorker work)
    {
        super();
        worker = work;
        receiver = work.getReceiver();
        workManager = receiver.getWorkManager();
        //use the class loader from the thread it created the work.
        classLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void run()
    {
        ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        try
        {
            try
            {
                Thread.currentThread().setContextClassLoader(classLoader);
                if (!worker.isRunning())
                {
                    workManager.scheduleWork(worker);
                }
            }
            catch (Exception e)
            {
                receiver.getEndpoint().getMuleContext().getExceptionListener().handleException(e);
            }
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

}
