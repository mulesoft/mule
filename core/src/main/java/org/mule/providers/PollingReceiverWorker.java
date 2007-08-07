/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import javax.resource.spi.work.Work;

public class PollingReceiverWorker implements Work
{
    private final AbstractPollingMessageReceiver receiver;

    public PollingReceiverWorker(AbstractPollingMessageReceiver pollingMessageReceiver)
    {
        super();
        receiver = pollingMessageReceiver;
    }

    // the run() method will exit after each poll() since it will be invoked again
    // by the scheduler
    public void run()
    {
        if (!receiver.stopped.get())
        {
            try
            {
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
        }
    }

    public void release()
    {
        // nop
    }

}
