/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.security;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
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

    public NotPermittedException(Message message, Throwable cause)
    {
        super(message, RequestContext.getEvent(), cause);
    }

    public NotPermittedException(Message message, MuleEvent event)
    {
        super(message, event);
    }

    public NotPermittedException(Message message, MuleEvent event, Throwable cause)
    {
        super(message, event, cause);
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
