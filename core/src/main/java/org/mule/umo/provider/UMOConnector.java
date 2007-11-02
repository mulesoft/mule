/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.provider;

import org.mule.impl.ManagementContextAware;
import org.mule.umo.MessagingException;
import org.mule.umo.NamedObject;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Lifecycle;

import java.beans.ExceptionListener;
import java.io.OutputStream;

/**
 * <code>UMOConnector</code> is the mechanism used to connect to external systems
 * and protocols in order to send and receive data.
 */
public interface UMOConnector extends Lifecycle, ManagementContextAware, NamedObject
{
    int INT_VALUE_NOT_SET = -1;

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
    UMOMessageReceiver registerListener(UMOComponent component, UMOImmutableEndpoint endpoint) throws Exception;

    /**
     * @param component the listening component
     * @param endpoint the associated endpointDescriptor with the listener
     * @throws Exception if the listener cannot be unregistered. If a listener is not
     *             associated with the given endpoint this will not throw an
     *             exception
     */
    void unregisterListener(UMOComponent component, UMOImmutableEndpoint endpoint) throws Exception;

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
     * Gets a {@link UMOMessageAdapter} from the connector for the given message
     * (data)
     * 
     * @param message the data with which to initialise the {@link UMOMessageAdapter}
     * @return the {@link UMOMessageAdapter} for the endpoint
     * @throws MessagingException if the message parameter is not supported
     * @see UMOMessageAdapter
     */
    UMOMessageAdapter getMessageAdapter(Object message) throws MessagingException;

    /**
     * @return the primary protocol name for endpoints of this connector
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

    boolean isRemoteSyncEnabled();
    
    /**
     * Used to define is this connectors endpoints' should be synchronous by default rather than using mule's
     * instance wide default. The endpoint is passed through to this method so that transports like axis/xfire
     * can determine if synchronous should be default depending on the endpoint transport e.g. http/vm/jms
     * etc.
     * 
     * @param endpoint
     * @return
     * @see UMOImmutableEndpoint#isSynchronous()
     */
    boolean isSyncEnabled(UMOImmutableEndpoint endpoint);

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
     * @param uri the endpoint uri to use when connecting to the resource
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     * @deprecated Use receive(UMOImmutableEndpoint endpoint, long timeout)
     */
    UMOMessage receive(String uri, long timeout) throws Exception;

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


    /**
     * Will get the output stream for this type of transport. Typically this
     * will be called only when Streaming is being used on an outbound endpoint.
     * If Streaming is not supported by this transport an {@link UnsupportedOperationException}
     * is thrown.   Note that the stream MUST release resources on close.  For help doing so, see
     * {@link org.mule.impl.model.streaming.CallbackOutputStream}.
     *
     * @param endpoint the endpoint that releates to this Dispatcher
     * @param message the current message being processed
     * @return the output stream to use for this request
     * @throws UMOException
     */
    OutputStream getOutputStream(UMOImmutableEndpoint endpoint, UMOMessage message) throws UMOException;

    UMOManagementContext getManagementContext();
}
