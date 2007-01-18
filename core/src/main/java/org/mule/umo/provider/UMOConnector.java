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

import org.mule.umo.MessagingException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.Registerable;

import java.beans.ExceptionListener;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <code>UMOConnector</code> is the mechanism used to connect to external systems
 * and protocols in order to send and receive data.
 */
public interface UMOConnector extends Disposable, Initialisable, Registerable
{
    public static final int INT_VALUE_NOT_SET = -1;

    /**
     * This creates a <code>UMOMessageReceiver</code> associated with this endpoint
     * and registers it with the connector
     * 
     * @param component the listening component
     * @param endpoint the endpoint contains the listener endpointUri on which to
     *            listen on.
     * @throws Exception if the UMOMessageReceiver cannot be created or the Receiver
     *             cannot be registered
     */
    UMOMessageReceiver registerListener(UMOComponent component, UMOEndpoint endpoint) throws Exception;

    /**
     * @param component the listening component
     * @param endpoint the associated endpointDescriptor with the listener
     * @throws Exception if the listener cannot be unregistered. If a listener is not
     *             associated with the given endpoint this will not throw an
     *             exception
     */
    void unregisterListener(UMOComponent component, UMOEndpoint endpoint) throws Exception;

    /**
     * @return true if the endpoint is started
     */
    boolean isStarted();

    /**
     * @return false if the connector is alive and well or true if the connector is
     *         being destroyed
     */
    boolean isDisposed();

    /**
     * @return false if the connector is alive and well or true if the connector has
     *         been told to dispose
     */
    boolean isDisposing();

    /**
     * Gets a {@link UMOMessageAdapter} from the connector for the given
     * message (data)
     * 
     * @param message the data with which to initialise the
     *            {@link UMOMessageAdapter}
     * @return the {@link UMOMessageAdapter} for the endpoint
     * @throws MessagingException if the message parameter is not supported
     * @see UMOMessageAdapter
     */
    UMOMessageAdapter getMessageAdapter(Object message) throws MessagingException;

    /**
     * Gets a {@link UMOStreamMessageAdapter} from the connector for the given
     * message. This Adapter will correctly handle data streaming for this type of
     * connector
     * 
     * @param in the input stream to read the data from
     * @param out the outputStream to write data to. This can be null.
     * @return the {@link UMOStreamMessageAdapter} for the endpoint
     * @throws MessagingException if the message parameter is not supported
     * @see UMOStreamMessageAdapter
     */
    UMOStreamMessageAdapter getStreamMessageAdapter(InputStream in, OutputStream out)
        throws MessagingException;

    /**
     * @return the name associated with the endpoint
     */
    String getName();

    /**
     * @param newName the name to associate with the endpoint
     */
    void setName(String newName);

    /**
     * @return the protocol name for the endpoint
     */
    String getProtocol();

    /**
     * @return true if the protocol is supported by this connector.
     */
    boolean supportsProtocol(String protocol);

    /**
     * @param listener the exception strategy to use with this endpoint
     * @see ExceptionListener
     */
    void setExceptionListener(ExceptionListener listener);

    /**
     * @return the Exception stategy used by the endpoint
     * @see ExceptionListener
     */
    ExceptionListener getExceptionListener();

    /**
     * @param exception the exception that was caught
     */
    void handleException(Exception exception);

    /**
     * The dispatcher factory is used to create a message dispatcher of the current
     * request
     * 
     * @param factory the factory to use when a dispatcher request is madr
     */
    void setDispatcherFactory(UMOMessageDispatcherFactory factory);

    /**
     * The dispatcher factory is used to create a message dispatcher of the current
     * request
     * 
     * @return the factory to use when a dispatcher request is madr
     */
    UMOMessageDispatcherFactory getDispatcherFactory();

    public void startConnector() throws UMOException;

    public void stopConnector() throws UMOException;

    public boolean isRemoteSyncEnabled();

    /**
     * If the underlying transport has the notion of a client session, such a session
     * should be obtainable using this method. The connector is free to associate
     * sessions with state, pool or create them etc.
     * 
     * @param endpoint the endpoint for which a session is needed
     * @param args additional argument(s) for obtaining a session. The exact type of
     *            argument and its interpretation is transport-dependent.
     * @return a transport-specific session or null if there is no session available
     * @throws UMOException
     */
    Object getDelegateSession(UMOImmutableEndpoint endpoint, Object args) throws UMOException;

    /**
     * Dispatches an event from the endpoint to the external system
     * 
     * @param event The event to dispatch
     * @throws DispatchException if the event fails to be dispatched
     */
    void dispatch(UMOImmutableEndpoint endpoint, UMOEvent event) throws DispatchException;

    /**
     * Make a specific request to the underlying transport
     * 
     * @param endpointUri the endpoint URI to use when connecting to the resource
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     * @deprecated Use receive(UMOImmutableEndpoint endpoint, long timeout)
     */
    UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception;

    /**
     * Make a specific request to the underlying transport
     * 
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    UMOMessage receive(UMOImmutableEndpoint endpoint, long timeout) throws Exception;

    /**
     * Sends an event from the endpoint to the external system
     * 
     * @param event The event to send
     * @return event the response form the external system wrapped in a UMOEvent
     * @throws DispatchException if the event fails to be dispatched
     */
    UMOMessage send(UMOImmutableEndpoint endpoint, UMOEvent event) throws DispatchException;

}
