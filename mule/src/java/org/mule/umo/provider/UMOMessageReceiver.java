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

import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.Lifecycle;

/**
 * <code>UMOMessageReceiver</code> is used to receive data from an external system.
 * Typically an implementation of this interface will also implement the listener interface
 * for the external system. For example to listen to a JMS destination the developer would
 * also implement javax.jms.MessageListener.  The endpoint (which creates the UMOMessageReceiver) will
 * then register the reciever with the JMS server. Where a listener interface is not availiable
 * the derived <code>UMOMessageReceiver</code> will implement the code necessary to receive data from
 * the external system.  For example, the file endpoint will poll a specified directory for it's data.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOMessageReceiver extends Lifecycle
{
    /**
     * @return the receivers endpoint
     */
    public abstract UMOEndpoint getEndpoint();

    /**
     * @param message
     * @param exception
     */
    //public abstract void handleException(Object message, Throwable exception);

    /**
     * @return the component associated with the receiver
     */
    public abstract UMOComponent getComponent();

    /**
     * @param endpoint the endpoint to listen on
     * @see UMOEndpoint
     */
    public abstract void setEndpoint(UMOEndpoint endpoint);

    /**
     * @param component the component to associate with the receiver.  When data is recieved the component
     *                  <code>dispatchEvent</code> or <code>sendEvent</code> is used to dispatch the data to the relivant UMO.
     */
    public abstract void setComponent(UMOComponent component);

    public abstract void setConnector(UMOConnector connector);

    public UMOConnector getConnector();

    /**
     * The endpointUri that this receiver listens on
     * @return
     */
    public UMOEndpointURI getEndpointURI();

    /**
     * Make the connection to the underlying transport.  Once a connection is made the Message
     * Reciever should remain in a stopped state if the MessageReceiver was stopped before
     * this method was called
     * @throws Exception
     */
    public void connect() throws Exception;

    /**
     * Disconnect the Message Receiver from the underlying transport
     *
     * @throws Exception
     */
    public void disconnect() throws Exception;

    /**
     * Determines if the the Message Receiver is connected or not
     * @return
     */
    public boolean isConnected();
}
