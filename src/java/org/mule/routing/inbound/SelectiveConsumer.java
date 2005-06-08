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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.management.stats.RouterStatistics;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.routing.UMOInboundRouter;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>SelectiveConsumer</code> is an inbound router used to filter out
 * unwanted events. The filtering is performed by a <code>UMOFilter</code>
 * that can be set on the router. If the event does not match the filter a
 * <code>UMOROutnerCatchAllStrategy</code> can be set on this router to route
 * unwanted events. If a catch strategy is not set the router just returns null.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class SelectiveConsumer implements UMOInboundRouter
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private UMOFilter filter;
    private boolean transformFirst = true;

    private RouterStatistics routerStatistics;

    public boolean isMatch(UMOEvent event) throws MessagingException
    {
        if (filter == null) {
            return true;
        }
        UMOMessage message;
        if (transformFirst) {
            try {
                Object payload = event.getTransformedMessage();
                message = new MuleMessage(payload, event.getMessage().getProperties());
            } catch (TransformerException e) {
                throw new RoutingException(new Message(Messages.TRANSFORM_FAILED_BEFORE_FILTER),
                                           event.getMessage(),
                                           event.getEndpoint(),
                                           e);
            }
        } else {
            message = event.getMessage();
        }
        return filter.accept(message);
    }

    public UMOEvent[] process(UMOEvent event) throws MessagingException
    {
        if (isMatch(event)) {
            return new UMOEvent[] { event };
        } else {
            return null;
        }
    }

    public UMOFilter getFilter()
    {
        return filter;
    }

    public void setFilter(UMOFilter filter)
    {
        this.filter = filter;
    }

    public boolean isTransformFirst()
    {
        return transformFirst;
    }

    public void setTransformFirst(boolean transformFirst)
    {
        this.transformFirst = transformFirst;
    }

    public void setRouterStatistics(RouterStatistics stats)
    {
        this.routerStatistics = stats;
    }

    public RouterStatistics getRouterStatistics()
    {
        return routerStatistics;
    }
}
