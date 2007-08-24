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

import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.Lifecycle;

import java.io.OutputStream;

/**
 * <code>UMOMessageReceiver</code> is used to receive data from an external system.
 * Typically an implementation of this interface will also implement the listener
 * interface for the external system. For example to listen to a JMS destination the
 * developer would also implement javax.jms.MessageListener. The endpoint (which
 * creates the UMOMessageReceiver) will then register the receiver with the JMS
 * server. Where a listener interface is not availiable the derived
 * <code>UMOMessageReceiver</code> will implement the code necessary to receive
 * data from the external system. For example, the file endpoint will poll a
 * specified directory for its data.
 */
public interface UMOMessageReceiver extends Lifecycle, UMOConnectable
{
    /**
     * @return the receivers endpoint
     */
    UMOEndpoint getEndpoint();

    /**
     * @param message
     * @param exception
     */
    // void handleException(Object message, Throwable exception);
    /**
     * @return the component associated with the receiver
     */
    UMOComponent getComponent();

    /**
     * @param endpoint the endpoint to listen on
     * @see UMOEndpoint
     */
    void setEndpoint(UMOEndpoint endpoint);

    /**
     * @param component the component to associate with the receiver. When data is
     *            received the component <code>dispatchEvent</code> or
     *            <code>sendEvent</code> is used to dispatch the data to the
     *            relivant UMO.
     */
    void setComponent(UMOComponent component);

    void setConnector(UMOConnector connector);

    UMOConnector getConnector();

    /**
     * The endpointUri that this receiver listens on
     * 
     * @return
     */
    UMOEndpointURI getEndpointURI();

    String getReceiverKey();

    void setReceiverKey(String key);

    UMOMessage routeMessage(UMOMessage message) throws UMOException;

    UMOMessage routeMessage(UMOMessage message, boolean synchronous) throws UMOException;

    UMOMessage routeMessage(UMOMessage message, UMOTransaction trans, boolean synchronous)
            throws UMOException;

    UMOMessage routeMessage(UMOMessage message, OutputStream outputStream) throws UMOException;

    UMOMessage routeMessage(UMOMessage message, boolean synchronous, OutputStream outputStream)
        throws UMOException;

    UMOMessage routeMessage(UMOMessage message,
                            UMOTransaction trans,
                            boolean synchronous,
                            OutputStream outputStream) throws UMOException;

}
