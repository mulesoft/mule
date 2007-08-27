/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.security;

import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.impl.RequestContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

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

    public UnauthorisedException(Message message, UMOMessage umoMessage)
    {
        super(message, umoMessage);
    }

    public UnauthorisedException(Message message, UMOMessage umoMessage, Throwable cause)
    {
        super(message, umoMessage, cause);
    }

    public UnauthorisedException(UMOMessage umoMessage,
                                 UMOSecurityContext context,
                                 UMOImmutableEndpoint endpoint,
                                 UMOEndpointSecurityFilter filter)
    {
        super(constructMessage(context, endpoint, filter), umoMessage);
    }

    private static Message constructMessage(UMOSecurityContext context,
                                            UMOImmutableEndpoint endpoint,
                                            UMOEndpointSecurityFilter filter)
    {

        Message m = null;
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
