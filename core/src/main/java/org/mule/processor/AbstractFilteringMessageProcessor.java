/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.FilterUnacceptedException;
import org.mule.config.i18n.CoreMessages;

/**
 * Abstract {@link InterceptingMessageProcessor} that can be easily be extended and
 * used for filtering message flow through a {@link MessageProcessor} chain. The
 * default behaviour when the filter is not accepted is to return the request event.
 */
public abstract class AbstractFilteringMessageProcessor extends AbstractInterceptingMessageProcessor
{
    /** 
     * Throw a FilterUnacceptedException when a message is rejected by the filter? 
     */
    protected boolean throwOnUnaccepted = false;
    protected boolean onUnacceptedFlowConstruct;

    
    /** 
     * The <code>MessageProcessor</code> that should be used to handle messages that are not accepted by the filter.
     */
    protected MessageProcessor unacceptedMessageProcessor;

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (accept(event))
        {
            return processNext(event);
        }
        else
        {
            return handleUnaccepted(event);
        }
    }

    protected abstract boolean accept(MuleEvent event);

    protected MuleEvent handleUnaccepted(MuleEvent event) throws MuleException
    {        
        if (unacceptedMessageProcessor != null)
        {
            return unacceptedMessageProcessor.process(event);
        }
        else if (throwOnUnaccepted)
        {
            throw filterUnacceptedException(event);
        }
        else
        {
            return null;
        }
    }

    protected MuleException filterUnacceptedException(MuleEvent event)
    {
        return new FilterUnacceptedException(CoreMessages.messageRejectedByFilter(), event);        
    }
    
    public MessageProcessor getUnacceptedMessageProcessor()
    {
        return unacceptedMessageProcessor;
    }

    public void setUnacceptedMessageProcessor(MessageProcessor unacceptedMessageProcessor)
    {
        this.unacceptedMessageProcessor = unacceptedMessageProcessor;
        if (unacceptedMessageProcessor instanceof FlowConstruct)
        {
            onUnacceptedFlowConstruct = true;
        }
    }

    public boolean isThrowOnUnaccepted()
    {
        return throwOnUnaccepted;
    }

    public void setThrowOnUnaccepted(boolean throwOnUnaccepted)
    {
        this.throwOnUnaccepted = throwOnUnaccepted;
    }
}
