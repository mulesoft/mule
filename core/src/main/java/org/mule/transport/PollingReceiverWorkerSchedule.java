/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;


public class PollingReceiverWorkerSchedule implements Runnable
{
    protected final AbstractPollingMessageReceiver receiver;

    protected PollingReceiverWorkerSchedule(AbstractPollingMessageReceiver receiver)
    {
        super();
        this.receiver = receiver;
    }

    public void run()
    {
        final RetryCallback callback = new RetryCallback()
        {
            public void doWork(RetryContext context) throws Exception
            {
                // Make sure we are connected
                receiver.connect();
                try
                {
                    receiver.poll();
                }
                catch (InterruptedException e)
                {
                    // stop polling
                    receiver.stop();
                }
            }

            public String getWorkDescription()
            {
                return receiver.getConnectionDescription();
            }
        };
        
        try
        {
            receiver.getConnector().getRetryPolicyTemplate().execute(callback);
        }
        catch (Exception e)
        {
            receiver.handleException(e);
        }
    }
}
