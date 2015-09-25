/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.security;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;

import java.net.URI;

/**
 * <code>NotPermittedException</code> is thrown if the user isn't authorized
 * to perform an action.
 */
public class NotPermittedException extends SecurityException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6664384216189042673L;

    public NotPermittedException(Message message)
    {
        super(message, RequestContext.getEvent());
    }

    public NotPermittedException(Message message, Throwable cause, MessageProcessor failingMessageProcessor)
    {
        super(message, RequestContext.getEvent(), cause, failingMessageProcessor);
    }

    public NotPermittedException(Message message, MuleEvent event)
    {
        super(message, event);
    }

    public NotPermittedException(Message message, MuleEvent event, Throwable cause, MessageProcessor failingMessageProcessor)
    {
        super(message, event, cause, failingMessageProcessor);
    }

    public NotPermittedException(MuleEvent event, SecurityContext context,SecurityFilter filter)
    {
        super(constructMessage(context, event.getMessageSourceURI(), filter), event);
    }

    private static Message constructMessage(SecurityContext context, 
                                            URI endpointURI,
                                            SecurityFilter filter)
    {

        Message m;
        if (context == null)
        {
            m = CoreMessages.authSetButNoContext(filter.getClass().getName());
        }
        else
        {
            m = CoreMessages.authFailedForUser(context.getAuthentication().getPrincipal());
        }
        m.setNextMessage(CoreMessages.authorizationDeniedOnEndpoint(endpointURI));
        return m;
    }
}
