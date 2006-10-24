/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.routing;

import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;

/**
 * <code>UMORouterCatchAllStrategy</code> TODO
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface UMORouterCatchAllStrategy
{
    void setEndpoint(UMOEndpoint endpoint);

    UMOEndpoint getEndpoint();

    UMOMessage catchMessage(UMOMessage message, UMOSession session, boolean synchronous)
        throws RoutingException;
}
