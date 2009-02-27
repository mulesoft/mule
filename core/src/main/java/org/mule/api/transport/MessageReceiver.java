/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.transport;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transaction.Transaction;

import java.io.OutputStream;

/**
 * <code>MessageReceiver</code> is used to receive data from an external system.
 * Typically an implementation of this interface will also implement the listener
 * interface for the external system. For example to listen to a JMS destination the
 * developer would also implement javax.jms.MessageListener. The endpoint (which
 * creates the MessageReceiver) will then register the receiver with the JMS
 * server. Where a listener interface is not availiable the derived
 * <code>MessageReceiver</code> will implement the code necessary to receive
 * data from the external system. For example, the file endpoint will poll a
 * specified directory for its data.
 */
public interface MessageReceiver extends Connectable
{
    /**
     * @return the endpoint from which we are receiving events 
     */
    InboundEndpoint getEndpoint();

    /**
     * @return the service associated with the receiver
     */
    Service getService();

    /**
     * @param endpoint the endpoint to listen on
     * @see ImmutableEndpoint
     */
    void setEndpoint(InboundEndpoint endpoint);

    /**
     * @param service the service to associate with the receiver. When data is
     *            received the service <code>dispatchEvent</code> or
     *            <code>sendEvent</code> is used to dispatch the data to the
     *            relevant component.
     */
    //void setService(Service service);

    //Connector getConnector();

    /**
     * The endpointUri that this receiver listens on
     * 
     * @return
     */
    EndpointURI getEndpointURI();

    String getReceiverKey();

    void setReceiverKey(String key);

    MuleMessage routeMessage(MuleMessage message) throws MuleException;

    MuleMessage routeMessage(MuleMessage message, boolean synchronous) throws MuleException;

    MuleMessage routeMessage(MuleMessage message, Transaction trans, boolean synchronous)
            throws MuleException;

    MuleMessage routeMessage(MuleMessage message, OutputStream outputStream) throws MuleException;

    MuleMessage routeMessage(MuleMessage message, boolean synchronous, OutputStream outputStream)
        throws MuleException;

    MuleMessage routeMessage(MuleMessage message,
                            Transaction trans,
                            boolean synchronous,
                            OutputStream outputStream) throws MuleException;
}
