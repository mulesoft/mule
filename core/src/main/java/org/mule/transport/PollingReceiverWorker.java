/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
                    receiver.getConnector().getMuleContext().getExceptionListener().handleException(e1);
                }
            }
            catch (MessagingException e)
            {
                //Already handled by TransactionTemplate
            }
            catch (Exception e)
            {
                receiver.getConnector().getMuleContext().getExceptionListener().handleException(e);
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
