/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.lifecycle.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.Startable;
import org.mule.config.i18n.CoreMessages;

public class ProcessIfStartedWaitIfSyncPausedMessageProcessor extends
    ProcessIfStartedWaitIfPausedMessageProcessor
{

    public ProcessIfStartedWaitIfSyncPausedMessageProcessor(Startable startable, LifecycleState lifecycleState)
    {
        super(startable, lifecycleState);
    }

    // TODO DF This needs refactoring. This is to ensure processNext()
    // is used and not next.process()
    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (accept(event))
        {
            if (isPaused() && event.getExchangePattern().hasResponse())
            {
                try
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(startable.getClass().getName() + " " + getStartableName(startable)
                                     + " is paused. Blocking call until resumd");
                    }
                    while (isPaused())
                    {
                        Thread.sleep(500);
                    }
                }
                catch (InterruptedException e)
                {
                    throw new MessagingException(
                        CoreMessages.interruptedWaitingForPaused(getStartableName(startable)), event, e, this);
                }
            }
            return processNext(event);
        }
        else
        {
            return handleUnaccepted(event);
        }
    }
}
