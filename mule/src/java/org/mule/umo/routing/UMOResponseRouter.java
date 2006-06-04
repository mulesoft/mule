/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.umo.routing;

import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;

/**
 * <code>UMOResponseRouter</code> is a router that handles response flow
 *
 * Response Agrregators are used to collect responses that are usually sent to replyTo endpoints set
 * on outbound routers. When an event is sent out via an outbound router, the response router will block the
 * response flow on an UMOComponent until the Response Router resolves a reply or times out.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface UMOResponseRouter extends UMORouter
{
    /**
     * This method is invoked when an event is received via an endpoint on this Response Router.
     * It is responsible for tieing up the event it receives with responses waiting to return back
     * to the callee.
     * This method will be called by a different thread to the getResponse method. The getResponse() method block
     * the response execution until the process method signals that a match is found.
     * @param event
     * @throws RoutingException
     */
    void process(UMOEvent event) throws RoutingException;

    /**
     * Called by the Mule framework once the outbound router has been processed on a component
     * the Message passed in is the response message from the component (or outbount router if a response
     * was returned). This method is invoked to signal that the event flow for the component has completed and
     * what ever message is returned from this method with be sent back as the response.
     *
     * This method will block until the correct response for the given Message has been received.
     *
     * @param message The processed message from the Component
     * @return the response message sent back to the callee
     * @throws RoutingException
     * @see UMOMessage
     * @see org.mule.umo.UMOComponent
     */
    UMOMessage getResponse(UMOMessage message) throws RoutingException;

    /**
     * Sets the timeout delay that the response router should wait for a response for a given event.  If the time
     * expires and exception will be thrown by Mule.
     * @param timeout the time in milliseconds to wait for a response event
     */
    void setTimeout(int timeout);

    /**
     * Returns the timeout delay that the response router should wait for a response for a given event.  If the time
     * expires and exception will be thrown by Mule.
     * @return the time in milliseconds to wait for a response event
     */
    int getTimeout();
}
