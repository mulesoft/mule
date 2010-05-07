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
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.StartException;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.transport.Connectable;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
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
    protected MuleMessageFactory muleMessageFactory = null;

    protected final WaitableBoolean connected = new WaitableBoolean(false);
    protected final WaitableBoolean connecting = new WaitableBoolean(false);
    protected final WaitableBoolean started = new WaitableBoolean(false);
    protected final AtomicBoolean disposed = new AtomicBoolean(false);

    /**
     * Indicates whether the receiver/dispatcher/requester should start upon connecting.  
     * This is necessary to support asynchronous retry policies, otherwise the start() 
     * method would block until connection is successful.
     */
    protected volatile boolean startOnConnect = false;

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
        initializeRetryPolicy();
        initializeMessageFactory();
    }

    protected void initializeRetryPolicy()
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

    protected void initializeMessageFactory() throws InitialisationException
    {
        try
        {
            muleMessageFactory = createMuleMessageFactory();
        }
        catch (CreateException ce)
        {
            Message message = MessageFactory.createStaticMessage(ce.getMessage());
            throw new InitialisationException(message, ce, this);
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
        // This method may be called to ensure transport is connected, if it is
        // already connected then just return.
        if (connected.get() || connecting.get())
        {
            return;
        }

        if (disposed.get())
        {
            throw new IllegalStateException("Requester/dispatcher has been disposed; cannot connect to resource");
        }
        
        if (!connecting.compareAndSet(false, true))
        {
            return;
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
                        connecting.set(false);

                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Connected: " + getWorkDescription());
                        }
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
                            logger.debug("exception in doWork", e);
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

        if (logger.isDebugEnabled())
        {
            logger.debug("Disconnected: " + this);
        }
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

    public final boolean isConnecting()
    {
        return connecting.get();
    }

    protected boolean isDoThreading ()
    {
        return connector.getDispatcherThreadingProfile().isDoThreading();
    }

    /**
     * Returns a string identifying the underlying resource
     */
    public String getConnectionDescription()
    {
        return "endpoint.outbound." + endpoint.getEndpointURI().toString();
    }

    /**
     * This method will start the connectable, calling {@link #connect()} if it is
     * needed.
     * <p>
     * This method is synchronous or not depending on how the {@link #retryTemplate}
     * behaves.
     * <p>
     * This method ensures that {@link #doStart()} will be called at most one time
     * and will return without error if the component is already {@link #started}.
     */
    public final void start() throws MuleException
    {
        if (!connected.get() && !connecting.get())
        {
            connectAndThenStart();
        }
        else
        {
            if (started.compareAndSet(false, true))
            {
                try
                {
                    retryTemplate.execute(new RetryCallback()
                    {
                        public void doWork(RetryContext context) throws InterruptedException, MuleException
                        {
                            callDoStartWhenItIsConnected();
                        }

                        public String getWorkDescription()
                        {
                            return "starting " + getConnectionDescription();
                        }
                    }, getWorkManager());
                }
                catch (MuleException e)
                {
                    throw e;
                }
                catch (Exception e)
                {
                    throw new StartException(CoreMessages.failedToStart("Connectable: " + this), e, this);
                }
            }
            else
            {
                logger.warn("Ignoring an attempt to start a connectable that is already started: " + this);
            }
        }
    }

    /**
     * This method will call {@link #connect()} after setting {@link #startOnConnect}
     * in true. This will make the {@link #connect()} method call {@link #start()}
     * after finishing establishing connection.
     * 
     * @throws LifecycleException
     */
    protected void connectAndThenStart() throws LifecycleException
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
    }

    /**
     * This method will block until {@link #connected} is true and then will call
     * {@link #doStart()}.
     * 
     * @throws InterruptedException if the thread is interrupted while waiting for
     *             {@link #connected} to be true.
     * @throws MuleException this is just a propagation of any {@link MuleException}
     *             that {@link #doStart()} may throw.
     */
    protected void callDoStartWhenItIsConnected() throws InterruptedException, MuleException
    {
        try
        {
            connected.whenTrue(new Runnable()
            {
                public void run()
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Starting: " + this);
                    }
                    try
                    {
                        doStart();
                    }
                    catch (MuleException e)
                    {
                        throw new MuleRuntimeException(
                            CoreMessages.createStaticMessage("wrapper exception for a MuleException"), e);
                    }
                }
            });
        }
        catch (MuleRuntimeException e)
        {
            if (e.getCause() instanceof MuleException)
            {
                throw (MuleException) e.getCause();
            }
            else
            {
                throw e;
            }
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

    @Override
    public String toString()
    {
        final StringBuffer sb = new StringBuffer(80);
        sb.append(ClassUtils.getSimpleName(this.getClass()));
        sb.append("{this=").append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(", endpoint=").append(endpoint.getEndpointURI());
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
    
    /**
     * This method uses the connector's <code>createMuleMessageFactory</code> method to create
     * a new {@link MuleMessageFactory}. Subclasses may need to override this method in order to 
     * perform additional initialization on the message factory before it's actually used.
     */
    protected MuleMessageFactory createMuleMessageFactory() throws CreateException
    {
        return connector.createMuleMessageFactory();
    }
    
    /**
     * Uses this object's {@link MuleMessageFactory} to create a new {@link MuleMessage} instance.
     * The payload of the new message will be taken from <code>transportMessage</code>, all
     * message properties will be copied from <code>previousMessage</code>.
     */
    public MuleMessage createMuleMessage(Object transportMessage, MuleMessage previousMessage,
        String encoding) throws MuleException
    {
        try
        {
            return muleMessageFactory.create(transportMessage, previousMessage, encoding);
        }
        catch (Exception e)
        {
            throw new CreateException(CoreMessages.failedToCreate("MuleMessage"), e);
        }
    }
 
    /**
     * Uses this object's {@link MuleMessageFactory} to create a new {@link MuleMessage} instance.
     * This is the designated way to build {@link MuleMessage}s from the transport specific message. 
     */
    public MuleMessage createMuleMessage(Object transportMessage, String encoding) throws MuleException
    {
        try
        {
            return muleMessageFactory.create(transportMessage, encoding);
        }
        catch (Exception e)
        {
            throw new CreateException(CoreMessages.failedToCreate("MuleMessage"), e);
        }
    }

    /**
     * Uses this object's {@link MuleMessageFactory} to create a new {@link MuleMessage} instance.
     * Uses the default encoding.
     * 
     * @see MuleConfiguration#getDefaultEncoding()
     */
    public MuleMessage createMuleMessage(Object transportMessage) throws MuleException
    {
        String encoding = endpoint.getMuleContext().getConfiguration().getDefaultEncoding();
        return createMuleMessage(transportMessage, encoding);
    }
    
    /**
     * Uses this object's {@link MuleMessageFactory} to create a new {@link MuleMessage} instance.
     * Rather than passing in a transport message instance, {@link NullPayload} is used instead.
     * Uses the default encoding.
     */
    protected MuleMessage createNullMuleMessage() throws MuleException
    {
        return createMuleMessage(null);
    }
}
