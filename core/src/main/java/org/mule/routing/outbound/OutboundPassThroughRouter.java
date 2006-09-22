/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.umo.UMOFilter;
import org.mule.umo.UMOImmutableDescriptor;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.RoutingException;

import java.util.List;

/**
 * <code>InboundPassThroughRouter</code> allows outbound routing over a single
 * endpoint without any filtering. This class is used by Mule when a single
 * outbound router is set on a UMODescriptor.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class OutboundPassThroughRouter extends FilteringOutboundRouter
{
    public OutboundPassThroughRouter()
    {
        super();
    }

    public OutboundPassThroughRouter(UMOImmutableDescriptor descriptor)
    {
        super();
        if (descriptor != null && descriptor.getOutboundEndpoint() != null) {
            addEndpoint(descriptor.getOutboundEndpoint());
        }
    }

    public void addEndpoint(UMOEndpoint endpoint)
    {
        if (endpoint == null) {
            return;
        }
        if (endpoints.size() == 1) {
            throw new IllegalArgumentException("Only one endpoint can be set on the PassThrough router");
        }
        super.addEndpoint(endpoint);
    }

    public void setEndpoints(List endpoints)
    {
        if (endpoints.size() > 1) {
            throw new IllegalArgumentException("Only one endpoint can be set on the PassThrough router");
        }
        super.setEndpoints(endpoints);
    }

    public void setFilter(UMOFilter filter)
    {
        throw new UnsupportedOperationException("The Pass Through cannot use filters, use the FilteringOutboundRouter instead");
    }

    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous) throws RoutingException
    {
        if (endpoints == null || endpoints.size() == 0) {
            return message;
        }
        return super.route(message, session, synchronous);
    }
}
