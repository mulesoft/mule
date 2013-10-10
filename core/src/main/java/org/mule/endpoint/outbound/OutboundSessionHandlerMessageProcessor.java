/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.SessionHandler;
import org.mule.util.ObjectUtils;

/**
 * Stores session information on the outbound message.
 *
 * @see SessionHandler
 */
public class OutboundSessionHandlerMessageProcessor implements MessageProcessor
{
    private SessionHandler sessionHandler;

    public OutboundSessionHandlerMessageProcessor(SessionHandler sessionHandler)
    {
        this.sessionHandler = sessionHandler;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        sessionHandler.storeSessionInfoToMessage(event.getSession(), event.getMessage());
        return event;
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
