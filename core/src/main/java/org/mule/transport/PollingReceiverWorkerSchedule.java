/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport;

import org.mule.api.context.WorkManager;

public class PollingReceiverWorkerSchedule implements Runnable
{
    protected final PollingReceiverWorker worker;
    protected final WorkManager workManager;
    protected final AbstractPollingMessageReceiver receiver;

    protected PollingReceiverWorkerSchedule(PollingReceiverWorker work)
    {
        super();
        worker = work;
        receiver = work.getReceiver();
        workManager = receiver.getWorkManager();
    }

    public void run()
    {
        try
        {
            if (!worker.isRunning())
            {
                workManager.scheduleWork(worker);
            }
        }
        catch (Exception e)
        {
            receiver.getConnector().getMuleContext().getExceptionListener().handleException(e);
        }
    }

}
