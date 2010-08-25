/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.security;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;

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

    /**
     * @deprecated use UnauthorisedException(Message, MuleEvent)
     */
    @Deprecated
    public UnauthorisedException(Message message, MuleMessage muleMessage)
    {
        super(message, muleMessage);
    }

    public UnauthorisedException(Message message, MuleEvent event)
    {
        super(message, event);
    }

    /**
     * @deprecated use 
     */
    @Deprecated
    public UnauthorisedException(Message message, MuleMessage muleMessage, Throwable cause)
    {
        super(message, muleMessage, cause);
    }

    public UnauthorisedException(Message message, MuleEvent event, Throwable cause)
    {
        super(message, event, cause);
    }

    /**
     * @deprecated use UnauthorisedException(MuleEvent, SecurityContext, ImmutableEndpoint, EndpointSecurityFilter)
     */
    @Deprecated
    public UnauthorisedException(MuleMessage message, SecurityContext context, 
        ImmutableEndpoint endpoint, EndpointSecurityFilter filter)
    {
        super(constructMessage(context, endpoint, filter), message);
    }

    public UnauthorisedException(MuleEvent event, SecurityContext context, 
        ImmutableEndpoint endpoint, EndpointSecurityFilter filter)
    {
        super(constructMessage(context, endpoint, filter), event);
    }

    private static Message constructMessage(SecurityContext context, ImmutableEndpoint endpoint,
        EndpointSecurityFilter filter)
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
        m.setNextMessage(CoreMessages.authDeniedOnEndpoint(endpoint.getEndpointURI()));
        return m;
    }
}
