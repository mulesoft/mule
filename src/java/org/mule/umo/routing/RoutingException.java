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

package org.mule.umo.routing;

import org.mule.umo.UMOException;

/**
 * <code>RoutingException</code> is a base class for al routing exceptions.  Routing
 * exceptions are only thrown for InboundMessageRouter and OutboundMessageRouter and
 * deriving types.  Mule itself does not throw routing exceptions when routing internal
 * events.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class RoutingException extends UMOException
{
    private Object event;

    public RoutingException(String message)
    {
        super(message);
    }

    public RoutingException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public RoutingException(String message, Throwable cause, Object event)
    {
        super(message, cause);
        this.event = event;
    }
}
