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
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;

import java.util.List;

/**
 * <code>UMOOutboundRouter</code> TODO
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public interface UMOOutboundRouter extends UMORouter
{
    public void setEndpoints(List endpoints);

    public List getEndpoints();

    public void addEndpoint(UMOEndpoint endpoint);

    public boolean removeEndpoint(UMOEndpoint endpoint);

    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous) throws RoutingException;

    public boolean isMatch(UMOMessage message) throws RoutingException;

}
