/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.DefaultMuleEvent;
import org.mule.management.stats.RouterStatistics;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.RoutingException;
import org.mule.api.routing.ServiceRoutingException;
import org.mule.config.i18n.CoreMessages;

/**
 * <code>ForwardingCatchAllStrategy</code> acts as a catch and forward router for
 * any events not caught by the router this strategy is associated with. Users can
 * assign an endpoint to this strategy to forward all events to. This can be used as
 * a dead letter/error queue.
 *
 */
public class ForwardingCatchAllStrategy extends AbstractCatchAllStrategy
{
    private boolean sendTransformed = false;

    protected OutboundEndpoint endpoint;

    public void setEndpoint(OutboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public OutboundEndpoint getEndpoint()
    {
        return endpoint;
    }

    public MuleMessage doCatchMessage(MuleMessage message, MuleSession session)
        throws RoutingException
    {
        if (getEndpoint() == null)
        {
            throw new ServiceRoutingException(CoreMessages.noCatchAllEndpointSet(), message,
                getEndpoint(), session.getService());
        }
        try
        {
            OutboundEndpoint endpoint = getEndpoint();
            if (sendTransformed && endpoint.getTransformers() != null)
            {
                message.applyTransformers(endpoint.getTransformers());
            }

            MuleEvent newEvent = new DefaultMuleEvent(message, endpoint, session, getEndpoint().isSynchronous());

            if (getEndpoint().isSynchronous())
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
            throw new RoutingException(message, getEndpoint(), e);

        }
    }

    public boolean isSendTransformed()
    {
        return sendTransformed;
    }

    public void setSendTransformed(boolean sendTransformed)
    {
        this.sendTransformed = sendTransformed;
    }
}
