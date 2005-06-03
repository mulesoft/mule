/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.umo.security;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.RequestContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * <code>UnauthorisedException</code> is thrown if authentication fails
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class UnauthorisedException extends SecurityException
{
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
        if (context == null) {
            m = new Message(Messages.AUTH_SET_TO_X_BUT_NO_CONTEXT, filter.getClass().getName());
        } else {
            m = new Message(Messages.AUTH_FAILED_FOR_USER_X, context.getAuthentication().getPrincipal());
        }
        m.setNextMessage(new Message(Messages.AUTH_DENIED_ON_ENDPOINT_X, endpoint.getEndpointURI()));
        return m;
    }
}
