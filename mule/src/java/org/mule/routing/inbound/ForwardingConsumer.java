/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing.inbound;

import org.mule.impl.MuleEvent;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.routing.UMOOutboundMessageRouter;

/**
 * <code>ForwardingConsumer</code> is used to forward an incoming event over
 * another transport without invoking a component. This can be used to implement
 * a bridge accross defferent transports.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ForwardingConsumer extends SelectiveConsumer
{
    public UMOEvent[] process(UMOEvent event) throws MessagingException
    {
        if (super.process(event) != null) {

            UMOOutboundMessageRouter router = event.getComponent().getDescriptor().getOutboundRouter();
            // Set the stopFurtherProcessing flag to true
            // to inform the InboundMessageRouter not to route
            // these events to the component
            event.setStopFurtherProcessing(true);
            if (router == null) {
                logger.debug("Descriptor has no outbound router configured to forward to, continuing with normal processing");
                return new UMOEvent[] { event };
            } else {
                try {
                    UMOMessage msg = router.route(event.getMessage(), event.getSession(), event.isSynchronous());
                    // what's the correct behaviour for async endpoints?
                    // maybe let router.route() return a Future for the returned
                    // msg?
                    if (msg != null) {
                        return new UMOEvent[] { new MuleEvent(msg, event) };
                    } else {
                        return null;
                    }

                } catch (UMOException e) {
                    throw new RoutingException(event.getMessage(), event.getEndpoint(), e);
                }
            }
        }
        return null;
    }
}
