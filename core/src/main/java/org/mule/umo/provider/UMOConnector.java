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
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;

import java.beans.ExceptionListener;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <code>UMOConnector</code> is the mechanism used to connect to external systems
 * and protocols in order to send and receive data.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOConnector extends Disposable, Initialisable
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
     * Gets a <code>UMOMessageAdapter</code> from the connector for the given
     * message (data)
     * 
     * @param message the data with which to initialise the
     *            <code>UMOMessageAdapter</code>
     * @return the <code>UMOMessageAdapter</code> for the endpoint
     * @throws MessagingException if the message parameter is not supported
     * @see UMOMessageAdapter
     */
    UMOMessageAdapter getMessageAdapter(Object message) throws MessagingException;

    /**
     * Gets a <code>UMOStreamMessageAdapter</code> from the connector for the given
     * message. This Adapter will correctly handle data streaming for this type of
     * connector
     * 
     * @param in the input stream to read the data from
     * @param out the outputStream to write data to. This can be null.
     * @return the <code>UMOStreamMessageAdapter</code> for the endpoint
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
     * The connector can pool dispatchers based on their endpointUri or can ingnore
     * the endpointUri altogether and use a ThreadLocal or always create new.
     * 
     * @param endpoint the endpoint that can be used to key cached dispatchers
     * @return the component associated with the endpointUri If there is no component
     *         for the current thread one will be created
     * @throws UMOException if creation of a component fails
     */
    UMOMessageDispatcher getDispatcher(UMOImmutableEndpoint endpoint) throws UMOException;

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
}
