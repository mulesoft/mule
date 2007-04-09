/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.provider;

import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;

/**
 * <code>UMOMessageDispatching</code> defines generic methods for dispatching and
 * receiving events. The exact behaviour of the action is defined by the implementing
 * class.
 * 
 * @see org.mule.umo.endpoint.UMOImmutableEndpoint
 * @see org.mule.umo.provider.UMOMessageDispatcher
 */
public interface UMOMessageDispatching
{
    long RECEIVE_WAIT_INDEFINITELY = 0;
    long RECEIVE_NO_WAIT = -1;

    /**
     * Dispatches an event from the endpoint to the external system
     * 
     * @param event The event to dispatch
     * @throws DispatchException if the event fails to be dispatched
     */
    void dispatch(UMOEvent event) throws DispatchException;

    /**
     * Sends an event from the endpoint to the external system
     * 
     * @param event The event to send
     * @return event the response form the external system wrapped in a UMOEvent
     * @throws DispatchException if the event fails to be dispatched
     */
    UMOMessage send(UMOEvent event) throws DispatchException;

    /**
     * Make a specific request to the underlying transport
     * 
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    UMOMessage receive(long timeout) throws Exception;

}
