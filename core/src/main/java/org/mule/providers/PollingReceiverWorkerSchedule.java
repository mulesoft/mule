/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.umo.manager.UMOWorkManager;

public class PollingReceiverWorkerSchedule implements Runnable
{
    protected final PollingReceiverWorker worker;
    protected final UMOWorkManager workManager;
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
            receiver.handleException(e);
        }
    }

}
