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

/**
 * <code>RoutePathNotFoundException</code> is thrown if a routing path for an event
 * cannot be found.  This can be caused if there is no (or no matching) endpoint for
 * the event to route through.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class RoutePathNotFoundException extends RoutingException
{
    public RoutePathNotFoundException(String message)
    {
        super(message);
    }

    public RoutePathNotFoundException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
