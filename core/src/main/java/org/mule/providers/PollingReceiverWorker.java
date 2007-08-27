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

import javax.resource.spi.work.Work;

public class PollingReceiverWorker implements Work
{
    protected final AbstractPollingMessageReceiver receiver;
    protected volatile boolean running = false;

    public PollingReceiverWorker(AbstractPollingMessageReceiver pollingMessageReceiver)
    {
        super();
        receiver = pollingMessageReceiver;
    }

    public AbstractPollingMessageReceiver getReceiver()
    {
        return receiver;
    }
    
    public boolean isRunning()
    {
        return running;
    }

    // the run() method will exit after each poll() since it will be invoked again
    // by the scheduler
    public void run()
    {
        if (!receiver.stopped.get())
        {
            try
            {
                running = true;
                // make sure we are connected, wait if necessary
                receiver.connected.whenTrue(null);
                receiver.poll();
            }
            catch (InterruptedException e)
            {
                // stop polling
                receiver.stop();
            }
            catch (Exception e)
            {
                receiver.handleException(e);
            }
            finally
            {
                running = false;
            }
        }
    }

    public void release()
    {
        // nop
    }

}
