/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
