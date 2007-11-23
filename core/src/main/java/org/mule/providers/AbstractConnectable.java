/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.internal.notifications.ConnectionNotification;
import org.mule.impl.internal.notifications.MessageNotification;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.ReceiveException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageRequester;
import org.mule.umo.provider.UMOConnectable;
import org.mule.util.ClassUtils;

import java.beans.ExceptionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provide a default dispatch (client) support for handling threads lifecycle and validation.
 */
public abstract class AbstractConnectable implements UMOConnectable, ExceptionListener
{

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    /**
     * Thread pool of Connector sessions
     */
    protected UMOWorkManager workManager = null;

    protected final UMOImmutableEndpoint endpoint;
    protected final AbstractConnector connector;

    protected boolean disposed = false;

    protected ConnectionStrategy connectionStrategy;

    protected volatile boolean connecting = false;
    protected volatile boolean connected = false;

    public AbstractConnectable(UMOImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
        this.connector = (AbstractConnector) endpoint.getConnector();

        connectionStrategy = endpoint.getConnectionStrategy();
        if (connectionStrategy instanceof AbstractConnectionStrategy)
        {
            // We don't want to do threading in the dispatcher because we're either
            // already running in a worker thread (asynchronous) or we need to
            // complete the operation in a single thread
            final AbstractConnectionStrategy connStrategy = (AbstractConnectionStrategy) connectionStrategy;
            if (connStrategy.isDoThreading())
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Overriding doThreading to false on " + connStrategy);
                }
                connStrategy.setDoThreading(false);
            }
        }

        if (isDoThreading())
        {
            try
            {
                workManager = connector.getDispatcherWorkManager();
            }
            catch (UMOException e)
            {
                disposeAndLogException();
                throw new MuleRuntimeException(CoreMessages.failedToStart("WorkManager"), e);
            }
        }
    }

    protected void disposeAndLogException()
    {
        try
        {
            dispose();
        }
        catch (Throwable t)
        {
            logger.error("Could not dispose of the message dispatcher!", t);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.util.ExceptionListener#onException(java.lang.Throwable)
     */
    public void exceptionThrown(Exception e)
    {
        try
        {
            getConnector().handleException(e);
        }
        finally
        {
            dispose();
        }
    }

    public boolean validate()
    {
        // by default a dispatcher/requester can be used unless disposed
        return !disposed;
    }

    public void activate()
    {
        // nothing to do by default
    }

    public void passivate()
    {
        // nothing to do by default
    }

    /**
     * Template method to destroy any resources held by the Message Dispatcher
     */
    public final synchronized void dispose()
    {
        if (!disposed)
        {
            try
            {
                try
                {
                    this.disconnect();
                }
                catch (Exception e)
                {
                    // TODO MULE-863: What should we really do?
                    logger.warn(e.getMessage(), e);
                }

                this.doDispose();

                if (workManager != null)
                {
                    workManager.dispose();
                }
            }
            finally
            {
                disposed = true;
            }
        }
    }

    public UMOConnector getConnector()
    {
        return connector;
    }

    public UMOImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }

    public synchronized void connect() throws Exception
    {
        if (disposed)
        {
            throw new IllegalStateException("Requester/dispatcher has been disposed; cannot connect to resource");
        }

        if (connected)
        {
            return;
        }

        if (!connecting)
        {
            connecting = true;

            if (logger.isDebugEnabled())
            {
                logger.debug("Connecting: " + this);
            }

            connectionStrategy.connect(this);

            logger.info("Connected: " + this);
            return;
        }

        try
        {
            //Make sure the connector has connected. If it is connected, this method does nothing
            connectionStrategy.connect(connector);

            this.doConnect();
            connected = true;
            connecting = false;

            connector.fireNotification(new ConnectionNotification(this, getConnectEventId(endpoint),
                ConnectionNotification.CONNECTION_CONNECTED));
        }
        catch (Exception e)
        {
            connected = false;
            connecting = false;

            connector.fireNotification(new ConnectionNotification(this, getConnectEventId(endpoint),
                ConnectionNotification.CONNECTION_FAILED));

            if (e instanceof ConnectException)
            {
                throw (ConnectException) e;
            }
            else
            {
                throw new ConnectException(e, this);
            }
        }
    }

    public synchronized void disconnect() throws Exception
    {
        if (!connected)
        {
            return;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Disconnecting: " + this);
        }

        this.doDisconnect();
        connected = false;

        logger.info("Disconnected: " + this);

        connector.fireNotification(new ConnectionNotification(this, getConnectEventId(endpoint),
            ConnectionNotification.CONNECTION_DISCONNECTED));
    }

    protected String getConnectEventId(UMOImmutableEndpoint endpoint)
    {
        return connector.getName() + ".dispatcher(" + endpoint.getEndpointURI().getUri() + ")";
    }

    public final boolean isConnected()
    {
        return connected;
    }

    protected boolean isDoThreading ()
    {
        return connector.getDispatcherThreadingProfile().isDoThreading();
    }

    /**
     * Returns a string identifying the underlying resource
     *
     * @return
     */
    public String getConnectionDescription()
    {
        return endpoint.getEndpointURI().toString();
    }

    public synchronized void reconnect() throws Exception
    {
        disconnect();
        connect();
    }

    protected abstract void doDispose();

    protected abstract void doConnect() throws Exception;

    protected abstract void doDisconnect() throws Exception;

    //  @Override
    public String toString()
    {
        final StringBuffer sb = new StringBuffer(80);
        sb.append(ClassUtils.getSimpleName(this.getClass()));
        sb.append("{this=").append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(", endpoint=").append(endpoint.getEndpointURI().getUri());
        sb.append(", disposed=").append(disposed);
        sb.append('}');
        return sb.toString();
    }
}