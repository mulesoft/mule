/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.routing.UMORouterCatchAllStrategy;
import org.mule.util.StringMessageUtils;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TestCatchAllStrategy implements UMORouterCatchAllStrategy
{
    private UMOEndpoint endpoint;

    public void setEndpoint(UMOEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public UMOEndpoint getEndpoint()
    {
        return endpoint;
    }

    public UMOMessage catchMessage(UMOMessage message, UMOSession session, boolean synchronous) throws RoutingException
    {
        System.out.println(StringMessageUtils.getBoilerPlate("Caught an event in the router!", '*', 40));
        return null;
    }
}
