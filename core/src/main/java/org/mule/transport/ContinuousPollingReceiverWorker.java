/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;


/**
 * Bypass the regular scheduling mechanism in order to minimize latency and maximize
 * throughput for transports which have low or no cost for performing a poll operation 
 * (such as an in-memory queue).
 */
public class ContinuousPollingReceiverWorker extends PollingReceiverWorker
{
    public ContinuousPollingReceiverWorker(AbstractPollingMessageReceiver pollingMessageReceiver)
    {
        super(pollingMessageReceiver);
    }

    @Override
    protected void poll() throws Exception
    {
        /*
         * We simply run our own polling loop all the time as long as the receiver is started. The
         * blocking wait defined by Connector.getQueueTimeout() will prevent this worker's receiver
         * thread from busy-waiting.
         */
        while (getReceiver().isStarted() && !getReceiver().isStopping())
        {
            super.poll();
        }
    }
}
