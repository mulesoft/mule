/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.api.MuleException;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.transport.Connectable;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.MessageFactory;
import org.mule.context.notification.ConnectionNotification;
import org.mule.util.ClassUtils;
import org.mule.util.concurrent.WaitableBoolean;

import java.beans.ExceptionListener;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provide a default dispatch (client) support for handling threads lifecycle and validation.
 */
public abstract class AbstractConnectable implements Connectable, ExceptionListener
{
    protected transient Log logger = LogFactory.getLog(getClass());

    protected ImmutableEndpoint endpoint;
    protected final AbstractConnector connector;
    protected RetryPolicyTemplate retryTemplate;

    protected final WaitableBoolean connected = new WaitableBoolean(false);
    protected final WaitableBoolean started = new WaitableBoolean(false);
    protected final AtomicBoolean disposed = new AtomicBoolean(false);

    /**
     * Indicates whether the receiver/dispatcher/requester should start upon connecting.  
     * This is necessary to support asynchronous retry policies, otherwise the start() 
     * method would block until connection is successful.
     */
    protected boolean startOnConnect = false;

    public AbstractConnectable(ImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
        this.connector = (AbstractConnector) endpoint.getConnector();
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
            handleException(e);
        }
        finally
        {
            dispose();
        }
    }

    public void handleException(Exception exception)
    {
        connector.handleException(exception, this);
    }

    public boolean validate()
    {
        // by default a dispatcher/requester can be used unless disposed
        return !disposed.get();
    }

    public void activate()
    {
        // nothing to do by default
    }

    public void passivate()
    {
        // nothing to do by default
    }
    
    public void initialise() throws InitialisationException
    {
        if (endpoint.getRetryPolicyTemplate() != null)
        {
            retryTemplate = endpoint.getRetryPolicyTemplate();
        }
        else
        {
            retryTemplate = connector.getRetryPolicyTemplate();
        }
    }

    /**
     * Template method to destroy any resources held by the Message Dispatcher
     */
    public synchronized void dispose()
    {
        if (!disposed.get())
        {
            try
            {
                this.disconnect();
                this.stop();
            }
            catch (Exception e)
            {
                // TODO MULE-863: What should we really do?
                logger.warn(e.getMessage(), e);
            }
        }
    }

    public Connector getConnector()
    {
        return connector;
    }

    public ImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }

    public final synchronized void connect() throws Exception
    {
        if (disposed.get())
        {
            throw new IllegalStateException("Requester/dispatcher has been disposed; cannot connect to resource");
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Connecting: " + this);
        }
            
        retryTemplate.execute(
            new RetryCallback()
            {
                public void doWork(RetryContext context) throws Exception
                {
                    try
                    {
                        doConnect();
                        connected.set(true);

                        logger.info("Connected: " + getWorkDescription());
                        // TODO Make this work somehow inside the RetryTemplate
                        //connector.fireNotification(new ConnectionNotification(this, getConnectEventId(endpoint),
                        //    ConnectionNotification.CONNECTION_CONNECTED));

                        if (startOnConnect)
                        {
                            start();
                        }
                    }
                    catch (Exception e)
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug(e);
                        }
                        throw e;
                    }
                }
    
                public String getWorkDescription()
                {
                    return getConnectionDescription();
                }
            }, 
            getWorkManager()
        );
    }

    public RetryContext validateConnection(RetryContext retryContext)
    {
        retryContext.setOk();
        return retryContext;
    }
    
    public final synchronized void disconnect() throws Exception
    {
        if (!connected.get())
        {
            return;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Disconnecting: " + this);
        }

        this.doDisconnect();
        connected.set(false);

        logger.info("Disconnected: " + this);
        connector.fireNotification(new ConnectionNotification(this, getConnectEventId(endpoint),
            ConnectionNotification.CONNECTION_DISCONNECTED));
    }

    protected String getConnectEventId(ImmutableEndpoint endpoint)
    {
        return connector.getName() + ".dispatcher(" + endpoint.getEndpointURI().getUri() + ")";
    }

    public final boolean isConnected()
    {
        return connected.get();
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
        return "endpoint.outbound." + endpoint.getEndpointURI().toString();
    }

    public final void start() throws MuleException
    {
        if(!connected.get())
        {
            startOnConnect = true;

            // Make sure we are connected
            try
            {
                connect();
            }
            catch (Exception e)
            {
                throw new LifecycleException(e, this);
            }
            return;
        }

        if (started.compareAndSet(false, true))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Starting: " + this);
            }
            doStart();
            // TODO It seems like a good idea to reset this variable here
            //startOnConnect = false;
        }
    }

    public final void stop()
    {
        try
        {
            if (connected.get())
            {
                disconnect();
            }
        }
        catch (Exception e)
        {
            // TODO MULE-863: What should we really do?
            logger.error(e.getMessage(), e);
        }

        if (started.compareAndSet(true, false))
        {
            try
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Stopping: " + this);
                }
                doStop();
            }
            catch (MuleException e)
            {
                // TODO MULE-863: What should we really do?
                logger.error(e.getMessage(), e);
            }

        }
    }

    protected void doInitialise() throws InitialisationException
    {
        // nothing to do by default
    }
    
    protected void doDispose()
    {
        // nothing to do by default
    }

    protected void doConnect() throws Exception
    {
        // nothing to do by default
    }

    protected void doDisconnect() throws Exception
    {
        // nothing to do by default
    }

    protected void doStart() throws MuleException
    {
        // nothing to do by default
    }

    protected void doStop() throws MuleException
    {
        // nothing to do by default
    }

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

    public void setEndpoint(ImmutableEndpoint endpoint)
    {
        if (endpoint == null)
        {
            throw new IllegalArgumentException("Endpoint cannot be null");
        }
        this.endpoint = endpoint;
    }
    
    abstract protected WorkManager getWorkManager();

    public boolean isStarted()
    {
        return started.get();
    }
}
