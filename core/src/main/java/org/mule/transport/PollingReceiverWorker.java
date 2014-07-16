/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;

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
    @Override
    public void run()
    {
        // Make sure we start with a clean slate.
        RequestContext.clear();
        if (receiver.isStarted())
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
                    receiver.stop();
                }
                catch (MuleException e1)
                {
                    receiver.getEndpoint().getMuleContext().getExceptionListener().handleException(e1);
                }
            }
            catch (MessagingException e)
            {
                //Already handled by TransactionTemplate
            }
            catch (Exception e)
            {
                receiver.getEndpoint().getMuleContext().getExceptionListener().handleException(e);
            }
            finally
            {
                running = false;
            }
        }
    }

    protected void poll() throws Exception
    {
        receiver.performPoll();
    }

    @Override
    public void release()
    {
        // nop
    }
}
