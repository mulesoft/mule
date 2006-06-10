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
 *
 */
package org.mule.umo.provider;

import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Disposable;

import java.io.OutputStream;

/**
 * <code>UMOMessageDispatcher</code> is the interface responsible for distpatching events to a particular transport.
 * It implements the client code necessary to write data to the underlying protocol.
 * The dispatcher also exposes a receive method that allows users to make specific calls to the underlying transport to
 * receive an event.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOMessageDispatcher extends Disposable, UMOConnectable
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
     * @param endpointUri the endpoint URI to use when connecting to the resource
     * @param timeout the maximum time the operation should block before returning. The call should
     * return immediately if there is data available. If no data becomes available before the timeout
     * elapses, null will be returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be returned if no data was
     * avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     * @deprecated Use receive(UMOImmutableEndpoint endpoint, long timeout)
     */
    UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception;

    /**
     * Make a specific request to the underlying transport
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout the maximum time the operation should block before returning. The call should
     * return immediately if there is data available. If no data becomes available before the timeout
     * elapses, null will be returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be returned if no data was
     * avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    UMOMessage receive(UMOImmutableEndpoint endpoint, long timeout) throws Exception;

    /**
     * If the underlying transport has the notion of a client session when writing to it, the session should be
     * obtainable using this method. If there is no session a null will be returned
     * @return the transport specific session or null if there is no session
     * @throws UMOException
     */
    Object getDelegateSession() throws UMOException;

    /**
     * Gets the connector for this dispatcher
     * @return the connector for this dispatcher
     */
    UMOConnector getConnector();

    /**
     * Determines if this dispatcher has been disposed. Once disposed a dispatcher cannot be used again
     * @return true if this dispatcher has been disposed, false otherwise
     */
    boolean isDisposed();

    /**
     * Well get the output stream (if any) for this type of transport.  Typically this will be called only when Streaming
     * is being used on an outbound endpoint
     * @param endpoint the endpoint that releates to this Dispatcher
     * @param message the current message being processed
     * @return the output stream to use for this request or null if the transport does not support streaming
     * @throws UMOException
     */
    OutputStream getOutputStream(UMOImmutableEndpoint endpoint, UMOMessage message) throws UMOException;
}
