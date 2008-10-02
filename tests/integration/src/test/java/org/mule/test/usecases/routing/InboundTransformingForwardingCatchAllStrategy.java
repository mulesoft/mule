/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.routing;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.RoutingException;
import org.mule.api.routing.ServiceRoutingException;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.AbstractCatchAllStrategy;
import org.mule.routing.ForwardingCatchAllStrategy;

public class InboundTransformingForwardingCatchAllStrategy extends ForwardingCatchAllStrategy
{
    public InboundTransformingForwardingCatchAllStrategy()
    {
        this.setSendTransformed(true);
    }
}
