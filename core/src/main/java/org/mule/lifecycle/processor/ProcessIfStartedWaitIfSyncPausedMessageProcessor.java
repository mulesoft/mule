/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
