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

import org.mule.umo.UMOMessage;

/**
 * <code>CouldNotRouteOutboundMessageException</code> thrown if Mule fails to
 * route the current outbound event
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class CouldNotRouteOutboundMessageException extends RoutingException
{
    private UMOMessage umoMessage;
    public CouldNotRouteOutboundMessageException(String errorMessage, UMOMessage message)
    {
        super(errorMessage);
        umoMessage = message;
    }

    public CouldNotRouteOutboundMessageException(String errorMessage, Throwable cause, UMOMessage message)
    {
        super(errorMessage, cause);
        umoMessage = message;
    }

    public UMOMessage getUmoMessage()
    {
        return umoMessage;
    }


}
