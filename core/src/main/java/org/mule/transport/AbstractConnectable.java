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

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.transport.Connectable;
import org.mule.api.transport.Connector;
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

    protected final AtomicBoolean disposing = new AtomicBoolean(false);
    protected final AtomicBoolean disposed = new AtomicBoolean(false);

    protected RetryPolicyTemplate retryTemplate;
    
    protected final AtomicBoolean connecting = new AtomicBoolean(false);
    protected final WaitableBoolean connected = new WaitableBoolean(false);

    protected final WaitableBoolean stopped = new WaitableBoolean(true);

    protected boolean asyncConnections = false;
    
    protected boolean startOnConnect = false;
    
    /**
     * For the new retry policies to work properly, the transport's connection/disconnection 
     * to/from the underlying resource must occur in the doConnect()/doDisconnect() methods 
     * and not in doSend()/doDispatch()/doRequest()/getMessages(). This flag (false by default) 
     * indicates whether this particular Receiver/Dispatcher/Requester complies with this requirement.
     * @see MULE-3754
     */
    protected boolean useStrictConnectDisconnect = false;
    
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
            getConnector().handleException(e);
        }
        finally
        {
            dispose();
        }
    }

    public void handleException(Exception exception)
    {
        if (exception instanceof ConnectException)
        {
            logger.info("Exception caught is a ConnectException, disconnecting receiver and invoking ReconnectStrategy");
            try
            {
                disconnect();
            }
            catch (Exception e)
            {
                connector.getExceptionListener().exceptionThrown(e);
            }
        }
        connector.getExceptionListener().exceptionThrown(exception);
        if (exception instanceof ConnectException)
        {
            try
            {
                logger.warn("Reconnecting after exception: " + exception.getMessage(), exception);
                connect();
            }
            catch (Exception e)
            {
                connector.getExceptionListener().exceptionThrown(e);
            }
        }
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

        retryTemplate.execute(new RetryCallback()
        {
            public void doWork(RetryContext context) throws Exception
            {
                try
                {
                    doConnect();
                }
                catch (Exception e)
                {
                    if (logger.isDebugEnabled())
                    {
                        e.printStackTrace();
                    }
                    throw e;
                }
                connected.set(true);
            }

            public String getWorkDescription()
            {
                return getConnectionDescription();
            }
        }, getWorkManager());
        
        if (startOnConnect)
        {
            start();
        }
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

    public synchronized void reconnect() throws Exception
    {
        disconnect();
        connect();
    }

    public final void start() throws MuleException
    {
        if(!connected.get())
        {
            startOnConnect = true;
            return;
        }

        if (stopped.compareAndSet(true, false))
        {
            doStart();
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

        if (stopped.compareAndSet(false, true))
        {
            try
            {
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

    /** 
     * Override this method to do any processing which needs to happen before connecting. 
     */
    protected void doPreConnect(MuleEvent event) throws Exception
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
}