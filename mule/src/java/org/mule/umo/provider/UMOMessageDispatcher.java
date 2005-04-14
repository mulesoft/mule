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
 *
 */
package org.mule.umo.provider;

import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.Disposable;

/**
 * @author Ross Mason
 *         <p/>
 *         //TODO document
 */
public interface UMOMessageDispatcher extends Disposable
{
    public static final long RECEIVE_WAIT_INDEFINITELY = 0;
    public static final long RECEIVE_NO_WAIT = -1;
    /**
     * Dispatches an event from the endpoint to the external system
     *
     * @param event The event to dispatch
     * @throws java.lang.Exception if the event fails to be dispatched
     */
    public void dispatch(UMOEvent event) throws Exception;

    /**
     * Sends an event from the endpoint to the external system
     *
     * @param event The event to send
     * @return event the response form the external system wrapped in a UMOEvent
     * @throws java.lang.Exception if the event fails to be dispatched
     */
    public UMOMessage send(UMOEvent event) throws Exception;

    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception;

    public Object getDelegateSession() throws UMOException;

    public UMOConnector getConnector();

    public boolean isDisposed();
}
