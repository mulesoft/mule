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
package org.mule.routing.inbound;

import org.mule.impl.MuleMessage;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.RoutingException;

/**
 * <code>ForwardingConsumer</code> is used to forward an incoming event over another
 * transport without invoking a component.  This can be used to implement
 * a bridge accross defferent transports.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class ForwardingConsumer extends SelectiveConsumer
{
    public UMOEvent[] process(UMOEvent event) throws RoutingException
    {
        if(super.process(event)!=null) {
            UMOEndpoint endpoint = event.getComponent().getDescriptor().getOutboundEndpoint();
            if(endpoint == null) {
                logger.debug("Descriptor has no outbound endpoint configured to forward to, continuing with normal processing");
                return new UMOEvent[]{event};
            } else {
                try
                {
                    //create an outound event with the outbound endpoint
                    UMOMessage returnMessage = new MuleMessage(event.getTransformedMessage(), null);
                    UMOEvent result = event.getSession().createOutboundEvent(returnMessage, endpoint, event);
                    logger.info("Forwarding event directly to: " + endpoint.getEndpointURI());
                    return new UMOEvent[]{result};
                } catch (UMOException e)
                {
                    throw new RoutingException("Failed to route event through " + endpoint, e);
                }
            }
        }
        return null;
    }
}
