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
package org.mule.routing.outbound;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.management.stats.RouterStatistics;
import org.mule.routing.AbstractRouterCollection;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.routing.UMOOutboundMessageRouter;
import org.mule.umo.routing.UMOOutboundRouter;

import java.util.Iterator;

/**
 * <code>OutboundMessageRouter</code> is a container of routers. An
 * OutboundMessageRouter must have atleast one router. By default the first matching
 * router is used to route an event though it is possible to match on all routers meaning
 * that the message will get sent over all matching routers.
 * 
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason </a>
 * @version $Revision$
 */

public class OutboundMessageRouter extends AbstractRouterCollection implements
        UMOOutboundMessageRouter {

    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory
            .getLog(OutboundMessageRouter.class);

    public OutboundMessageRouter() {
        super(RouterStatistics.TYPE_OUTBOUND);
    }
    
    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous)
            throws RoutingException {

        UMOMessage result = null;
        boolean matchfound = false;
        //synchronized(lock) {
            for (Iterator iterator = getRouters().iterator(); iterator.hasNext();) {

                UMOOutboundRouter umoOutboundRouter = (UMOOutboundRouter) iterator.next();
                if (umoOutboundRouter.isMatch(message)) {
                    matchfound = true;
                    result = umoOutboundRouter.route(message, session, synchronous);

                    if (!isMatchAll()) {
                        return result;
                    }
                }
            }
        //}

        if (!matchfound && getCatchAllStrategy() != null) {
            logger.debug("Message did not match any routers on: "
                    + session.getComponent().getDescriptor().getName()
                    + " invoking catch all strategy");
            return catchAll(message, session, synchronous);
        } else if(!matchfound) {
            logger.warn("Message did not match any routers on: "
                            + session.getComponent().getDescriptor().getName()
                            + " and there is no catch all strategy configured on this router.  Disposing message.");
        }
        return message;
    }

    protected UMOMessage catchAll(UMOMessage message, UMOSession session, boolean synchronous)
            throws RoutingException {

        if (getStatistics().isEnabled()) {
            //getStatistics().incrementNoRoutedMessage();
            getStatistics().incrementCaughtMessage();
        }

        return getCatchAllStrategy().catchMessage(message, null, false);
    }
}