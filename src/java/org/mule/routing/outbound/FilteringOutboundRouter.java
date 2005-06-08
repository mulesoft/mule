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

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.umo.UMOException;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.CouldNotRouteOutboundMessageException;
import org.mule.umo.routing.RoutePathNotFoundException;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;

/**
 * <code>FilteringRouter</code> is a router that accepts events based on a
 * filter set.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class FilteringOutboundRouter extends AbstractOutboundRouter
{
    private UMOTransformer transformer;

    private UMOFilter filter;

    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous) throws RoutingException
    {
        UMOMessage result = null;
        if (endpoints == null || endpoints.size() == 0) {
            throw new RoutePathNotFoundException(new Message(Messages.NO_ENDPOINTS_FOR_ROUTER), message, null);
        }
        try {

            if (synchronous) {
                result = send(session, message, (UMOEndpoint) endpoints.get(0));
            } else {
                dispatch(session, message, (UMOEndpoint) endpoints.get(0));
            }
        } catch (UMOException e) {
            throw new CouldNotRouteOutboundMessageException(message, (UMOEndpoint) endpoints.get(0), e);
        }
        return result;
    }

    public UMOFilter getFilter()
    {
        return filter;
    }

    public void setFilter(UMOFilter filter)
    {
        this.filter = filter;
    }

    public boolean isMatch(UMOMessage message) throws RoutingException
    {
        if (getFilter() == null) {
            return true;
        }
        if (transformer != null) {
            try {
                Object payload = transformer.transform(message.getPayload());
                message = new MuleMessage(payload, message.getProperties());
            } catch (TransformerException e) {
                throw new RoutingException(new Message(Messages.TRANSFORM_FAILED_BEFORE_FILTER),
                                           message,
                                           (UMOEndpoint) endpoints.get(0),
                                           e);
            }
        }
        return getFilter().accept(message);
    }

    public UMOTransformer getTransformer()
    {
        return transformer;
    }

    public void setTransformer(UMOTransformer transformer)
    {
        this.transformer = transformer;
    }
}
