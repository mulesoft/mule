/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
