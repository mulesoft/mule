/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.umo.provider;

import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.RoutingException;
import org.mule.config.i18n.Message;

/**
 * <code>DispatchException</code> is thrown when an endpoint dispatcher fails to
 * send, dispatch or receive a message.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class DispatchException extends RoutingException
{
    public DispatchException(UMOMessage message, UMOEndpoint endpoint)
    {
        super(message, endpoint);
    }

    public DispatchException(UMOMessage umoMessage, UMOEndpoint endpoint, Throwable cause)
    {
        super(umoMessage, endpoint, cause);
    }

    public DispatchException(Message message, UMOMessage umoMessage, UMOEndpoint endpoint)
    {
        super(message, umoMessage, endpoint);
    }

    public DispatchException(Message message, UMOMessage umoMessage, UMOEndpoint endpoint, Throwable cause)
    {
        super(message, umoMessage, endpoint, cause);
    }
}
