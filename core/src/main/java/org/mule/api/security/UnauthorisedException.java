/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.security;

import org.mule.RequestContext;
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
    private static final long serialVersionUID = -6664384216189042672L;

    public UnauthorisedException(Message message)
    {
        super(message, RequestContext.getEventContext().getMessage());
    }

    public UnauthorisedException(Message message, Throwable cause)
    {
        super(message, RequestContext.getEventContext().getMessage(), cause);
    }

    public UnauthorisedException(Message message, MuleMessage muleMessage)
    {
        super(message, muleMessage);
    }

    public UnauthorisedException(Message message, MuleMessage muleMessage, Throwable cause)
    {
        super(message, muleMessage, cause);
    }

    public UnauthorisedException(MuleMessage message,
                                 SecurityContext context,
                                 ImmutableEndpoint endpoint,
                                 EndpointSecurityFilter filter)
    {
        super(constructMessage(context, endpoint, filter), message);
    }

    private static Message constructMessage(SecurityContext context,
                                            ImmutableEndpoint endpoint,
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
