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
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.routing.UMOOutboundMessageRouter;

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
            UMOOutboundMessageRouter router = event.getComponent().getDescriptor().getOutboundRouter();

            if(endpoint == null && router==null) {

                logger.debug("Descriptor has no outbound endpoint configured to forward to, continuing with normal processing");
                return new UMOEvent[]{event};
            } else  {
                try
                {
                    if(router!=null) {
                        //this isn't ideal as the request will execute in this thread
                        //and will not return a result in sync mode
                        router.route(event.getMessage(), event.getSession(), event.isSynchronous());
                        return null;
                    } else {
                        UMOEvent[] results = new UMOEvent[1];
                        results[0] = event.getSession().createOutboundEvent(
                                new MuleMessage(event.getTransformedMessage(), event.getProperties())
                                , endpoint, event);
                        logger.info("Forwarding event directly to: " + endpoint.getEndpointURI());
                        return results;
                    }
                } catch (UMOException e)
                {
                    throw new RoutingException("Failed to route event through " + endpoint, e);
                }
            }
        }
        return null;
    }
}
