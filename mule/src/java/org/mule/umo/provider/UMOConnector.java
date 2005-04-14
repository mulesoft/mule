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

import org.mule.umo.MessagingException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.Startable;
import org.mule.umo.lifecycle.Stoppable;

import java.beans.ExceptionListener;

/**
 * <code>UMOConnector</code> is the mechanism used to connect to external
 * systems and protocols in order to send and receive data.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOConnector extends Startable, Stoppable, Disposable, Initialisable
{
    /**
     * This creates a <code>UMOMessageReceiver</code> associated with this endpoint and registers it
     * with the endpoint
     *
     * @param component the listening component
     * @param endpoint  the endpoint contains the listener endpointUri on which to listen on.
     * @throws Exception if the UMOMessageReceiver cannot be created or the Receiver cannot be registered
     */
    public UMOMessageReceiver registerListener(UMOComponent component, UMOEndpoint endpoint) throws Exception;

    /**
     * @param component the listening component
     * @param endpoint the associated endpointDescriptor with the listener
     * @throws Exception if the listener cannot be unregistered.  If a listener is not
     *                   associated with the given endpoint this will not throw an exception
     */
    public void unregisterListener(UMOComponent component, UMOEndpoint endpoint) throws Exception;

    /**
     * @return true if the endpoint is started
     */
    public boolean isStarted();

    /**
     * @return true if the endpoint is alive and well or false if the endpoint is being destroyed
     */
    public boolean isDisposed();

    /**
     * Gets a <code>UMOMessageAdapter</code> for the endpoint for the given message (data)
     *
     * @param message the data with which to initialise the <code>UMOMessageAdapter</code>
     * @return the <code>UMOMessageAdapter</code> for the endpoint
     * @throws MessagingException if the message parameter is not supported
     * @see UMOMessageAdapter
     */
    public UMOMessageAdapter getMessageAdapter(Object message) throws MessagingException;

    /**
     * @return the name associated with the endpoint
     */
    public String getName();

    /**
     * @param newName the name to associate with the endpoint
     */
    public void setName(String newName);

    /**
     * @return the protocol name for the endpoint
     */
    public String getProtocol();

    /**
     * The connector can pool dispatchers based on their endpointUri or can ingnore
     * the endpointUri altogether and use a ThreadLocal or always create new.
     * @param endpoint the endpointUri that can be used to key cached dispatchers
     * @return the component associated with the endpointUri
     *         If there is no component for the current thread one will be created
     * @throws UMOException if creation of a component fails
     */
    public UMOMessageDispatcher getDispatcher(String endpoint) throws UMOException;

    /**
     * @param listener the exception strategy to use with this endpoint
     * @see UMOExceptionStrategy
     */
    public void setExceptionListener(ExceptionListener listener);

    /**
     * @return the Exception stategy used by the endpoint
     * @see UMOExceptionStrategy
     */
    public ExceptionListener getExceptionListener();

    /**
     * @param exception the exception that was caught
     */
    public void handleException(Exception exception);

    /**
     * The dispatcher factory is used to create a message dispatcher of the current
     * request
     * @param factory the factory to use when a dispatcher request is madr
     */
    public void setDispatcherFactory(UMOMessageDispatcherFactory factory);

    /**
     * The dispatcher factory is used to create a message dispatcher of the current
     * request
     * @return the factory to use when a dispatcher request is madr
     */
    public UMOMessageDispatcherFactory getDispatcherFactory();
}