/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import org.mule.api.MuleContext;
import org.mule.api.exception.MessageRedeliveredException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.api.processor.MessageProcessor;

import java.util.ArrayList;
import java.util.List;

public class RollbackMessagingExceptionStrategy extends TemplateMessagingExceptionStrategy
{
    private RedeliveryExceeded redeliveryExceeded;
    private Integer maxRedeliveryAttempts;


    @Override
    protected void doInitialise(MuleContext muleContext) throws InitialisationException
    {
        LifecycleUtils.initialiseIfNeeded(redeliveryExceeded);
        super.doInitialise(muleContext);
    }

    public void setRedeliveryExceeded(RedeliveryExceeded redeliveryExceeded)
    {
        this.redeliveryExceeded = redeliveryExceeded;
    }

    public void setMaxRedeliveryAttempts(Integer maxRedeliveryAttempts)
    {
        this.maxRedeliveryAttempts = maxRedeliveryAttempts;
    }

    public Integer getMaxRedeliveryAttempts()
    {
        return maxRedeliveryAttempts;
    }

    public boolean hasMaxRedeliveryAttempts()
    {
        return this.maxRedeliveryAttempts != null;
    }

    @Override
    protected MuleEvent beforeRouting(Exception exception, MuleEvent event)
    {        
        if (!isRedeliveryExhausted(exception))
        {
            rollback(exception);
        }
        return event;
    }

    @Override
    protected List<MessageProcessor> getOwnedMessageProcessors()
    {
        List<MessageProcessor> messageProcessors = new ArrayList<MessageProcessor>(super.getMessageProcessors().size() + (redeliveryExceeded == null ? 0 : redeliveryExceeded.getMessageProcessors().size()));
        messageProcessors.addAll(super.getMessageProcessors());
        if (redeliveryExceeded != null)
        {
            messageProcessors.addAll(redeliveryExceeded.getMessageProcessors());
        }
        return messageProcessors;
    }

    private boolean isRedeliveryExhausted(Exception exception)
    {
        return (exception instanceof MessageRedeliveredException);
    }

    /**
     * Always accept MessageRedeliveryException exceptions if this rollback exception strategy handles redelivery.
     */
    @Override
    protected boolean acceptsEvent(MuleEvent event)
    {
        return event.getMessage().getExceptionPayload().getException() instanceof MessageRedeliveredException && this.hasMaxRedeliveryAttempts();
    }

    @Override
    protected MuleEvent route(MuleEvent event, Exception t)
    {
        MuleEvent resultEvent = event;
        if (isRedeliveryExhausted(t))
        {
            if (redeliveryExceeded != null)
            {
                try
                {
                    markExceptionAsHandled(t);
                    resultEvent = redeliveryExceeded.process(event);
                }
                catch (MuleException e)
                {
                    logFatal(event, t);
                }
            }
            else
            {
                logger.info("Message redelivery exhausted. No redelivery exhausted actions configured. Message consumed.");
            }
        }
        else
        {
            resultEvent = super.route(event, t);
        }
        return resultEvent;
    }

    @Override
    protected void processReplyTo(MuleEvent event, Exception e)
    {
        if (isRedeliveryExhausted(e))
        {
            super.processReplyTo(event, e);
        }
    }

}
