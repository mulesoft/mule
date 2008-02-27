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

public class InboundTransformingForwardingCatchAllStrategy extends AbstractCatchAllStrategy
{
    public MuleMessage catchMessage(MuleMessage message, MuleSession session, boolean synchronous)
        throws RoutingException
    {
        OutboundEndpoint endpoint = this.getEndpoint();

        if (endpoint == null)
        {
            throw new ServiceRoutingException(
                CoreMessages.noCatchAllEndpointSet(), message, this.getEndpoint(), session.getService());
        }
        try
        {
            message = new DefaultMuleMessage(RequestContext.getEventContext().transformMessage(), message);
            MuleEvent newEvent = new DefaultMuleEvent(message, endpoint, session, synchronous);

            if (synchronous)
            {
                MuleMessage result = endpoint.send(newEvent);
                if (statistics != null)
                {
                    statistics.incrementRoutedMessage(getEndpoint());
                }
                return result;
            }
            else
            {
                endpoint.dispatch(newEvent);
                if (statistics != null)
                {
                    statistics.incrementRoutedMessage(getEndpoint());
                }
                return null;
            }

        }
        catch (Exception e)
        {
            throw new RoutingException(message, endpoint, e);
        }
    }
}
