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
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.NamedObject;
import org.mule.api.component.Component;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.Lifecycle;

import java.beans.ExceptionListener;
import java.io.OutputStream;

/**
 * <code>Connector</code> is the mechanism used to connect to external systems
 * and protocols in order to send and receive data.
 */
public interface Connector extends Lifecycle, MuleContextAware, NamedObject
{
    int INT_VALUE_NOT_SET = -1;

    /**
     * This creates a <code>MessageReceiver</code> associated with this endpoint
     * and registers it with the connector
     * 
     * @param component the listening component
     * @param endpoint the endpoint contains the listener endpointUri on which to
     *            listen on.
     * @throws Exception if the MessageReceiver cannot be created or the Receiver
     *             cannot be registered
     */
    MessageReceiver registerListener(Component component, ImmutableEndpoint endpoint) throws Exception;

    /**
     * @param component the listening component
     * @param endpoint the associated endpointDescriptor with the listener
     * @throws Exception if the listener cannot be unregistered. If a listener is not
     *             associated with the given endpoint this will not throw an
     *             exception
     */
    void unregisterListener(Component component, ImmutableEndpoint endpoint) throws Exception;

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
     * Gets a {@link MessageAdapter} from the connector for the given message
     * (data)
     * 
     * @param message the data with which to initialise the {@link MessageAdapter}
     * @return the {@link MessageAdapter} for the endpoint
     * @throws MessagingException if the message parameter is not supported
     * @see MessageAdapter
     */
    MessageAdapter getMessageAdapter(Object message) throws MessagingException;

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
    void setDispatcherFactory(MessageDispatcherFactory factory);

    /**
     * The dispatcher factory is used to create a message dispatcher of the current
     * request
     * 
     * @return the factory to use when a dispatcher request is madr
     */
    MessageDispatcherFactory getDispatcherFactory();

    /**
     * The requester factory is used to create a message requester of the current
     * request
     *
     * @param factory the factory to use when a request is made
     */
    void setRequesterFactory(MessageRequesterFactory factory);

    /**
     * The requester factory is used to create a message requester of the current
     * request
     *
     * @return the factory to use when a request is made
     */
    MessageRequesterFactory getRequesterFactory();

    boolean isRemoteSyncEnabled();
    
    /**
     * Used to define is this connectors endpoints' should be synchronous by default rather than using mule's
     * instance wide default. The endpoint is passed through to this method so that transports like axis/xfire
     * can determine if synchronous should be default depending on the endpoint transport e.g. http/vm/jms
     * etc.
     * 
     * @param endpoint
     * @return
     * @see ImmutableEndpoint#isSynchronous()
     */
    boolean isSyncEnabled(ImmutableEndpoint endpoint);

    /**
     * Dispatches an event from the endpoint to the external system
     * 
     * @param event The event to dispatch
     * @throws DispatchException if the event fails to be dispatched
     */
    void dispatch(ImmutableEndpoint endpoint, MuleEvent event) throws DispatchException;

    /**
     * Make a specific request to the underlying transport
     *
     * @param uri the endpoint uri to use when connecting to the resource
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a MuleMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     * @deprecated Use request(ImmutableEndpoint endpoint, long timeout)
     */
    MuleMessage request(String uri, long timeout) throws Exception;

    /**
     * Make a specific request to the underlying transport
     *
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a MuleMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    MuleMessage request(ImmutableEndpoint endpoint, long timeout) throws Exception;

    /**
     * Sends an event from the endpoint to the external system
     * 
     * @param event The event to send
     * @return event the response form the external system wrapped in a MuleEvent
     * @throws DispatchException if the event fails to be dispatched
     */
    MuleMessage send(ImmutableEndpoint endpoint, MuleEvent event) throws DispatchException;


    /**
     * Will get the output stream for this type of transport. Typically this
     * will be called only when Streaming is being used on an outbound endpoint.
     * If Streaming is not supported by this transport an {@link UnsupportedOperationException}
     * is thrown.   Note that the stream MUST release resources on close.  For help doing so, see
     * {@link org.mule.model.streaming.CallbackOutputStream}.
     *
     * @param endpoint the endpoint that releates to this Dispatcher
     * @param message the current message being processed
     * @return the output stream to use for this request
     * @throws MuleException
     */
    OutputStream getOutputStream(ImmutableEndpoint endpoint, MuleMessage message) throws MuleException;

    MuleContext getMuleContext();
}
