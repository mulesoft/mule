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

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.management.stats.RouterStatistics;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.UMOOutboundRouter;

import java.util.List;

/**
 * <code>AbstractOutboundRouter</code> is a base router class that tracks statics about message
 * processing through the router.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractOutboundRouter implements UMOOutboundRouter
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected List endpoints = new CopyOnWriteArrayList();

    protected String replyTo = null;

    protected RouterStatistics routerStatistics;


    public void dispatch(UMOSession session, UMOMessage message, UMOEndpoint endpoint) throws UMOException
    {
        if(replyTo!=null) {
            message.setReplyTo(replyTo);
            message.setProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY, session.getComponent().getDescriptor().getName());
        }

        session.dispatchEvent(message, endpoint);
        if(routerStatistics!=null) {
            if(routerStatistics.isEnabled() ) {
                routerStatistics.incrementRoutedMessage(endpoint);
            }
        }
    }

    public UMOMessage send(UMOSession session, UMOMessage message, UMOEndpoint endpoint) throws UMOException
    {
        if(replyTo!=null) {
            message.setProperty(MuleProperties.MULE_REPLY_TO_PROPERTY, replyTo);
            dispatch(session, message, endpoint);
            return null;
        }

        UMOMessage result = session.sendEvent(message, endpoint);
        if(routerStatistics!=null) {
            if(routerStatistics.isEnabled() ) {
                routerStatistics.incrementRoutedMessage(endpoint);
            }
        }
        return result;
    }

    public List getEndpoints()
    {
        return endpoints;
    }

    public void setEndpoints(List endpoints)
    {
        this.endpoints = new CopyOnWriteArrayList(endpoints);
    }

    public void addEndpoint(UMOEndpoint endpoint)
    {
        endpoint.setType(UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoints.add(endpoint);
    }

    public boolean removeEndpoint(UMOEndpoint endpoint)
    {
        return endpoints.remove(endpoint);
    }

    public String getReplyTo()
    {
        return replyTo;
    }

    public void setReplyTo(String replyTo)
    {
        if(replyTo!=null) {
            this.replyTo = MuleManager.getInstance().lookupEndpointIdentifier(replyTo, replyTo);
        } else {
            this.replyTo = null;
        }
    }

    public RouterStatistics getRouterStatistics()
    {
        return routerStatistics;
    }

    public void setRouterStatistics(RouterStatistics routerStatistics)
    {
        this.routerStatistics = routerStatistics;
    }
}
