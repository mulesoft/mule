/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.TransformationService;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.LifecycleStateEnabled;
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

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provide a default dispatch (client) support for handling threads lifecycle and validation.
 */
public abstract class AbstractTransportMessageHandler<O> implements Connectable, LifecycleStateEnabled
{
    protected transient Log logger = LogFactory.getLog(getClass());

    protected ImmutableEndpoint endpoint;
    protected final AbstractConnector connector;
    protected RetryPolicyTemplate retryTemplate;
    protected MuleMessageFactory muleMessageFactory = null;

    protected ConnectableLifecycleManager<O> lifecycleManager;
    // TODO This state info. needs to be incorporated into the ConnectableLifecycleManager
    protected final AtomicBoolean connected = new AtomicBoolean(false);
    
    public AbstractTransportMessageHandler(ImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
        this.connector = (AbstractConnector) endpoint.getConnector();
        this.lifecycleManager = createLifecycleManager();
    }

    protected abstract ConnectableLifecycleManager<O> createLifecycleManager();

    public LifecycleState getLifecycleState()
    {
        return lifecycleManager.getState();
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

    public boolean validate()
    {
        // by default a dispatcher/requester can be used unless disposed
        return !getLifecycleState().isDisposed();
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
        try
        {
            lifecycleManager.fireInitialisePhase(new LifecycleCallback<O>()
            {
                public void onTransition(String phaseName, O object) throws MuleException
                {
                    initializeRetryPolicy();
                    initializeMessageFactory();
                    doInitialise();
                }
            });
        }
        catch (InitialisationException e)
        {
            throw e;
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }

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

    /**
     * Subclasses can override this method to create a custom {@link MuleMessageFactory} instead
     * of re-using the instance from the connector.
     */
    protected void initializeMessageFactory() throws InitialisationException
    {
        try
        {
            muleMessageFactory = connector.getMuleMessageFactory();
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
        try
        {
            if (isStarted())
            {
                stop();
            }
            if (isConnected())
            {
                disconnect();
            }
        }
        catch (Exception e)
        {
            logger.warn(e.getMessage(), e);
        }

        try
        {
            lifecycleManager.fireDisposePhase(new LifecycleCallback<O>() 
            {
                public void onTransition(String phaseName, O object) throws MuleException
                {
                    doDispose();
                }
            });
        }
        catch (MuleException e)
        {
            logger.warn(e.getMessage(), e);
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
        if (connected.get())
        {
            return;
        }

        if (getLifecycleState().isDisposed())
        {
            throw new IllegalStateException(
                    "Requester/dispatcher has been disposed; cannot connect to resource:" + this);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Connecting: " + this);
        }

        connectHandler();
        connected.set(true);

        if (logger.isDebugEnabled())
        {
            logger.debug("Connected: " + getConnectionDescription());
        }
    }

    //TODO - This template method belongs to AbstractMessageReceiver. Not possible to move it in mule 3.x - bc compatibility.
    protected void connectHandler() throws Exception
    {
        this.doConnect();
    }

    public RetryContext validateConnection(RetryContext retryContext)
    {
        retryContext.setOk();
        return retryContext;
    }

    public final synchronized void disconnect() throws Exception
    {
        if (isStarted())
        {
            stop();
        }
        
        if (logger.isDebugEnabled())
        {
            logger.debug("Disconnecting: " + this);
        }

        doDisconnect();
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

    protected boolean isDoThreading()
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
     * <p/>
     * This method is synchronous or not depending on how the {@link #retryTemplate}
     * behaves.
     * <p/>
     * This method ensures that {@link #doStart()} will be called at most one time
     * and will return without error if the component is already started.
     */
    public final void start() throws MuleException
    {
        if (isStarted() || isStarting())
        {
            return;
        }

        if (!isConnected())
        {
            try
            {
                connect();
            }
            catch (MuleException me)
            {
                throw me;
            }
            catch (Exception e)
            {
                throw new ConnectException(e, this);
            }
        }

        lifecycleManager.fireStartPhase(new LifecycleCallback<O>()
        {
            public void onTransition(String phaseName, O object) throws MuleException
            {
                doStartHandler();
            }
        });
    }

    protected void doStartHandler() throws MuleException
    {
        doStart();
    }

    public final void stop() throws MuleException
    {
        lifecycleManager.fireStopPhase(new LifecycleCallback<O>()
        {
            public void onTransition(String phaseName, O object) throws MuleException
            {
                try
                {
                    doStop();
                }
                catch (MuleException e)
                {
                    logger.error(e.getMessage(), e);
                }
            }
        });

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
        final StringBuilder sb = new StringBuilder(80);
        sb.append(ClassUtils.getSimpleName(this.getClass()));
        sb.append("{this=").append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(", endpoint=").append(endpoint.getEndpointURI());
        sb.append(", disposed=").append(getLifecycleState().isDisposed());
        sb.append('}');
        return sb.toString();
    }

    // TODO MULE-4871 Endpoint should not be mutable

    public void setEndpoint(ImmutableEndpoint endpoint)
    {
        if (endpoint == null)
        {
            throw new IllegalArgumentException("Endpoint cannot be null");
        }
        this.endpoint = endpoint;
    }

    abstract protected WorkManager getWorkManager() throws MuleException;

    public boolean isStarted()
    {
        return getLifecycleState().isStarted();
    }

    public boolean isStarting()
    {
        return getLifecycleState().isStarting();
    }

    public boolean isStopped()
    {
        return getLifecycleState().isStopped();
    }

    public boolean isStopping()
    {
        return getLifecycleState().isStopping();
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
            return muleMessageFactory.create(transportMessage, previousMessage, encoding, endpoint.getMuleContext());
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
            return muleMessageFactory.create(transportMessage, encoding, endpoint.getMuleContext());
        }
        catch (Exception e)
        {
            throw new CreateException(CoreMessages.failedToCreate("MuleMessage"), e, this);
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

    protected TransformationService getTransformationService()
    {
        return connector.getMuleContext().getTransformationService();
    }

}
