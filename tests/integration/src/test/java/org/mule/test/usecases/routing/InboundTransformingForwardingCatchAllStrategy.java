/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.routing;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.routing.AbstractCatchAllStrategy;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.ComponentRoutingException;
import org.mule.umo.routing.RoutingException;

public class InboundTransformingForwardingCatchAllStrategy extends AbstractCatchAllStrategy
{

    public UMOMessage catchMessage(UMOMessage message, UMOSession session, boolean synchronous)
        throws RoutingException
    {
        UMOEndpoint endpoint = this.getEndpoint();

        if (endpoint == null)
        {
            throw new ComponentRoutingException(new Message(Messages.NO_CATCH_ALL_ENDPOINT_SET), message,
                getEndpoint(), session.getComponent());
        }
        try
        {
            message = new MuleMessage(RequestContext.getEventContext().getTransformedMessage(), message);
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
            throw new RoutingException(message, endpoint, e);
        }
    }
}
