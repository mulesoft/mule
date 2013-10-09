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
 * <code>UnauthorisedException</code> is thrown if authentication fails
 */

public class UnauthorisedException extends SecurityException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6664384216189042673L;

    public UnauthorisedException(Message message)
    {
        super(message, RequestContext.getEvent());
    }

    public UnauthorisedException(Message message, Throwable cause)
    {
        super(message, RequestContext.getEvent(), cause);
    }

    public UnauthorisedException(Message message, MuleEvent event)
    {
        super(message, event);
    }

    public UnauthorisedException(Message message, MuleEvent event, Throwable cause)
    {
        super(message, event, cause);
    }

    public UnauthorisedException(MuleEvent event, SecurityContext context, SecurityFilter filter)
    {
        super(constructMessage(context, event.getMessageSourceURI(), filter), event);
    }

    @Deprecated
    public UnauthorisedException(MuleEvent event, SecurityContext context, 
        URI endpointURI, SecurityFilter filter)
    {
        super(constructMessage(context, endpointURI, filter), event);
    }

    private static Message constructMessage(SecurityContext context, URI endpointURI,
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
        m.setNextMessage(CoreMessages.authDeniedOnEndpoint(endpointURI));
        return m;
    }
}
