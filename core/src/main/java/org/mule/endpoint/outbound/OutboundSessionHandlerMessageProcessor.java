/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
