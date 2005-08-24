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
package org.mule.routing.outbound;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.management.stats.RouterStatistics;
import org.mule.routing.AbstractRouterCollection;
import org.mule.transaction.TransactionCallback;
import org.mule.transaction.TransactionTemplate;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.routing.UMOOutboundMessageRouter;
import org.mule.umo.routing.UMOOutboundRouter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <code>OutboundMessageRouter</code> is a container of routers. An
 * OutboundMessageRouter must have atleast one router. By default the first
 * matching router is used to route an event though it is possible to match on
 * all routers meaning that the message will get sent over all matching routers.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason </a>
 * @version $Revision$
 */

public class OutboundMessageRouter extends AbstractRouterCollection implements UMOOutboundMessageRouter
{

    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(OutboundMessageRouter.class);

    public OutboundMessageRouter()
    {
        super(RouterStatistics.TYPE_OUTBOUND);
    }

    public UMOMessage route(final UMOMessage message, final UMOSession session, final boolean synchronous)
            throws MessagingException
    {

        UMOMessage result = null;
        boolean matchfound = false;

        for (Iterator iterator = getRouters().iterator(); iterator.hasNext();) {
            UMOOutboundRouter umoOutboundRouter = (UMOOutboundRouter) iterator.next();
            if (umoOutboundRouter.isMatch(message)) {
                matchfound = true;
                // Manage outbound only transactions here
                final UMOOutboundRouter router = umoOutboundRouter;
                TransactionTemplate tt = new TransactionTemplate(umoOutboundRouter.getTransactionConfig(),
                                                                 session.getComponent()
                                                                        .getDescriptor()
                                                                        .getExceptionListener());

                TransactionCallback cb = new TransactionCallback() {
                    public Object doInTransaction() throws Exception
                    {
                        return router.route(message, session, synchronous);
                    }
                };
                try {
                    result = (UMOMessage) tt.execute(cb);
                } catch (Exception e) {
                    throw new RoutingException(message, null, e);
                }

                if (!isMatchAll()) {
                    return result;
                }
            }
        }

        if (!matchfound && getCatchAllStrategy() != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Message did not match any routers on: "
                        + session.getComponent().getDescriptor().getName() + " invoking catch all strategy");
            }
            return catchAll(message, session, synchronous);
        } else if (!matchfound) {
            logger.warn("Message did not match any routers on: " + session.getComponent().getDescriptor().getName()
                    + " and there is no catch all strategy configured on this router.  Disposing message.");
        }
        return message;
    }

    /**
     * A helper method for finding out which endpoints a message would be routed
     * to without actually routing the the message
     * 
     * @param message the message to retrieve endpoints for
     * @return an array of UMOEndpoint objects or an empty array
     * @throws RoutingException
     */
    public UMOEndpoint[] getEndpointsForMessage(UMOMessage message) throws MessagingException
    {
        List endpoints = new ArrayList();
        for (Iterator iterator = getRouters().iterator(); iterator.hasNext();) {

            UMOOutboundRouter umoOutboundRouter = (UMOOutboundRouter) iterator.next();
            if (umoOutboundRouter.isMatch(message)) {
                endpoints.addAll(umoOutboundRouter.getEndpoints());
                if (!isMatchAll()) {
                    break;
                }
            }
        }
        UMOEndpoint[] result = new UMOEndpoint[endpoints.size()];
        return (UMOEndpoint[]) endpoints.toArray(result);
    }

    protected UMOMessage catchAll(UMOMessage message, UMOSession session, boolean synchronous) throws RoutingException
    {

        if (getStatistics().isEnabled()) {
            getStatistics().incrementCaughtMessage();
        }

        return getCatchAllStrategy().catchMessage(message, null, false);
    }

    public boolean hasEndpoints() {
        for (Iterator iterator = routers.iterator(); iterator.hasNext();) {
            UMOOutboundRouter router = (UMOOutboundRouter) iterator.next();
            if(router.getEndpoints().size() > 0 || router.isDynamicEndpoints()) return true;
        }
        return false;
    }

}
