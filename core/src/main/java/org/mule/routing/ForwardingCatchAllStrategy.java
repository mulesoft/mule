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

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.ComponentRoutingException;
import org.mule.umo.routing.RoutingException;

/**
 * <code>ForwardingCatchAllStrategy</code> acts as a catch and forward router for
 * any events not caught by the router this strategy is associated with. Users can
 * assign an endpoint to this strategy to forward all events to. This can be used as
 * a dead letter/error queue.
 */

public class ForwardingCatchAllStrategy extends AbstractCatchAllStrategy
{
    private boolean sendTransformed = false;

    public UMOMessage catchMessage(UMOMessage message, UMOSession session, boolean synchronous)
        throws RoutingException
    {
        if (getEndpoint() == null)
        {
            throw new ComponentRoutingException(CoreMessages.noCatchAllEndpointSet(), message,
                getEndpoint(), session.getComponent());
        }
        try
        {
            UMOEndpoint endpoint = getEndpoint();
            
            if (sendTransformed && endpoint.getTransformer() != null)
            {
                Object payload = message.getPayload();
                payload = endpoint.getTransformer().transform(payload);
                message = new MuleMessage(payload, message);
            }

            UMOEvent newEvent = new MuleEvent(message, endpoint, session, synchronous);

            if (synchronous)
            {
                UMOMessage result = endpoint.send(newEvent);
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
