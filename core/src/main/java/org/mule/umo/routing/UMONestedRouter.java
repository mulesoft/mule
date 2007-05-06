/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.routing;

import org.mule.umo.MessagingException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;

public interface UMONestedRouter extends UMORouter
{

    /**
     * This method is responsible for routing the Message via the Session. The logic
     * for this method will change for each type of router depending on expected
     * behaviour. For example, a MulticastingRouter might just iterate through the
     * list of assoaciated endpoints sending the message. Another type of router such
     * as the ExceptionBasedRouter will hit the first endpoint, if it fails try the
     * second, and so on. Most router implementations will extends the
     * FilteringOutboundRouter which implements all the common logic need for a
     * router.
     *
     * @param message the message to send via one or more endpoints on this router
     * @param session the session used to actually send the event
     * @param synchronous whether the invocation process should be synchronous or not
     * @return a result message if any from the invocation. If the synchronous flag
     *         is false a null result will always be returned.
     * @throws MessagingException if any errors occur during the sending of messages
     * @see org.mule.routing.outbound.FilteringOutboundRouter
     * @see org.mule.routing.outbound.ExceptionBasedRouter
     * @see org.mule.routing.outbound.MulticastingRouter
     */
    UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous) throws MessagingException;

    void setEndpoint(UMOEndpoint endpoint);

    UMOOutboundRouter getOutboundRouter();

    void setOutboundRouter(UMOOutboundRouter router);

    Class getInterface();

    void setInterface(Class interfaceClass);

    String getMethod();

    void setMethod(String method);

    /**
     * This wires the dynamic proxy to the service object.
     *
     * @param target
     */
    Object createProxy(Object target);
}
