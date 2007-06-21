/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.RegistryContext;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.NullSessionHandler;
import org.mule.impl.RequestContext;
import org.mule.impl.ResponseOutputStream;
import org.mule.impl.internal.notifications.ConnectionNotification;
import org.mule.impl.internal.notifications.MessageNotification;
import org.mule.impl.internal.notifications.SecurityNotification;
import org.mule.registry.DeregistrationException;
import org.mule.registry.RegistrationException;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.umo.security.SecurityException;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ClassUtils;
import org.mule.util.StringMessageUtils;
import org.mule.util.concurrent.WaitableBoolean;

import java.io.OutputStream;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractMessageReceiver</code> provides common methods for all Message
 * Receivers provided with Mule. A message receiver enables an endpoint to receive a
 * message from an external system.
 */
public abstract class AbstractMessageReceiver implements UMOMessageReceiver
{
    /** logger used by this class */
    protected final Log logger = LogFactory.getLog(getClass());

    /** The Component with which this receiver is associated with */
    protected UMOComponent component = null;

    /** The endpoint descriptor which is associated with this receiver */
    protected UMOEndpoint endpoint = null;

    private InternalMessageListener listener;

    /** the connector associated with this receiver */
    protected AbstractConnector connector = null;

    protected final AtomicBoolean disposing = new AtomicBoolean(false);

    protected final WaitableBoolean connected = new WaitableBoolean(false);

    protected final WaitableBoolean stopped = new WaitableBoolean(true);

    protected final AtomicBoolean connecting = new AtomicBoolean(false);

    /**
     * Stores the key to this receiver, as used by the Connector to
     * store the receiver.
     */
    protected String receiverKey = null;

    /**
     * Stores the endpointUri that this receiver listens on. This enpoint can be
     * different to the endpointUri in the endpoint stored on the receiver as
     * endpoint endpointUri may get rewritten if this endpointUri is a wildcard
     * endpointUri such as jms.*
     */
    private UMOEndpointURI endpointUri;

    private UMOWorkManager workManager;

    protected ConnectionStrategy connectionStrategy;

    protected String registryId = null;

    /**
     * Creates the Message Receiver
     *
     * @param connector the endpoint that created this listener
     * @param component the component to associate with the receiver. When data is
     *                  received the component <code>dispatchEvent</code> or
     *                  <code>sendEvent</code> is used to dispatch the data to the
     *                  relivant UMO.
     * @param endpoint  the provider contains the endpointUri on which the receiver
     *                  will listen on. The endpointUri can be anything and is specific to
     *                  the receiver implementation i.e. an email address, a directory, a
     *                  jms destination or port address.
     * @see UMOComponent
     * @see UMOEndpoint
     */
    public AbstractMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint)
            throws CreateException
    {
        setConnector(connector);
        setComponent(component);
        setEndpoint(endpoint);

        listener = new DefaultInternalMessageListener();
        endpointUri = endpoint.getEndpointURI();

        try
        {
            workManager = this.connector.getReceiverWorkManager("receiver");
        }
        catch (UMOException e)
        {
            throw new CreateException(e, this);
        }

        connectionStrategy = this.endpoint.getConnectionStrategy();

        try
        {
            register();
        }
        catch (RegistrationException re)
        {
            logger.error("Unable to register: " + re.toString());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Registerable#register()
     */
    public void register() throws RegistrationException
    {
        registryId =
                RegistryContext.getRegistry().registerMuleObject(connector, this).getId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Registerable#deregister()
     */
    public void deregister() throws DeregistrationException
    {
        RegistryContext.getRegistry().deregisterComponent(registryId);
        registryId = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Registerable#getRegistryId()
     */
    public String getRegistryId()
    {
        return registryId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOMessageReceiver#getEndpointName()
     */
    public UMOEndpoint getEndpoint()
    {
        return endpoint;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOMessageReceiver#getExceptionListener()
     */
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
                connectionStrategy.connect(this);
            }
            catch (UMOException e)
            {
                connector.getExceptionListener().exceptionThrown(e);
            }
        }
    }

    /**
     * This method is used to set any additional aand possibly transport specific
     * information on the return message where it has an exception payload.
     *
     * @param message
     * @param exception
     */
    protected void setExceptionDetails(UMOMessage message, Throwable exception)
    {
        String propName = ExceptionHelper.getErrorCodePropertyName(connector.getProtocol());
        // If we dont find a error code property we can assume there are not
        // error code mappings for this connector
        if (propName != null)
        {
            String code = ExceptionHelper.getErrorMapping(connector.getProtocol(), exception.getClass());
            if (logger.isDebugEnabled())
            {
                logger.debug("Setting error code for: " + connector.getProtocol() + ", " + propName + "="
                        + code);
            }
            message.setProperty(propName, code);
        }
    }

    public UMOConnector getConnector()
    {
        return connector;
    }

    public void setConnector(UMOConnector connector)
    {
        if (connector != null)
        {
            if (connector instanceof AbstractConnector)
            {
                this.connector = (AbstractConnector) connector;
            }
            else
            {
                throw new IllegalArgumentException(CoreMessages.propertyIsNotSupportedType(
                        "connector", AbstractConnector.class, connector.getClass()).getMessage());
            }
        }
        else
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("connector").getMessage());
        }
    }

    public UMOComponent getComponent()
    {
        return component;
    }

    public final UMOMessage routeMessage(UMOMessage message) throws UMOException
    {
        return routeMessage(message, (endpoint.isSynchronous() || TransactionCoordination.getInstance()
                .getTransaction() != null));
    }

    public final UMOMessage routeMessage(UMOMessage message, boolean synchronous) throws UMOException
    {
        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        return routeMessage(message, tx, tx != null || synchronous, null);
    }

    public final UMOMessage routeMessage(UMOMessage message, UMOTransaction trans, boolean synchronous)
            throws UMOException
    {
        return routeMessage(message, trans, synchronous, null);
    }

    public final UMOMessage routeMessage(UMOMessage message, OutputStream outputStream) throws UMOException
    {
        return routeMessage(message, endpoint.isSynchronous(), outputStream);
    }

    public final UMOMessage routeMessage(UMOMessage message, boolean synchronous, OutputStream outputStream)
            throws UMOException
    {
        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        return routeMessage(message, tx, tx != null || synchronous, outputStream);
    }

    public final UMOMessage routeMessage(UMOMessage message,
                                         UMOTransaction trans,
                                         boolean synchronous,
                                         OutputStream outputStream) throws UMOException
    {

        if (connector.isEnableMessageEvents())
        {
            connector.fireNotification(new MessageNotification(message, endpoint, component.getDescriptor()
                    .getName(), MessageNotification.MESSAGE_RECEIVED));
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Message Received from: " + endpoint.getEndpointURI());
        }
        if (logger.isTraceEnabled())
        {
            try
            {
                logger.trace("Message Payload: \n"
                        + StringMessageUtils.truncate(StringMessageUtils.toString(message.getPayload()),
                        200, false));
            }
            catch (Exception e)
            {
                // ignore
            }
        }

        // Apply the endpoint filter if one is configured
        if (endpoint.getFilter() != null)
        {
            if (!endpoint.getFilter().accept(message))
            {
                //TODO RM* This ain't pretty, we don't yet have an event context since the message hasn't gon to the 
                //message listener yet. So we need to create a new context so that EventAwareTransformers can be applied
                //to response messages where the filter denied the message
                //Maybe the filter should be checked in the MessageListener...
                RequestContext.setEvent(new MuleEvent(message, endpoint,
                        new MuleSession(message, new NullSessionHandler()), synchronous));
                return handleUnacceptedFilter(message);
            }
        }
        return listener.onMessage(message, trans, synchronous, outputStream);
    }

    protected UMOMessage handleUnacceptedFilter(UMOMessage message)
    {
        String messageId;
        messageId = message.getUniqueId();

        if (logger.isDebugEnabled())
        {
            logger.debug("Message " + messageId + " failed to pass filter on endpoint: " + endpoint
                    + ". Message is being ignored");
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOMessageReceiver#setEndpoint(org.mule.umo.endpoint.UMOEndpoint)
     */
    public void setEndpoint(UMOEndpoint endpoint)
    {
        if (endpoint == null)
        {
            throw new IllegalArgumentException("Endpoint cannot be null");
        }
        this.endpoint = endpoint;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOMessageReceiver#setSession(org.mule.umo.UMOSession)
     */
    public void setComponent(UMOComponent component)
    {
        if (component == null)
        {
            throw new IllegalArgumentException("Component cannot be null");
        }
        this.component = component;
    }

    public final void dispose()
    {
        stop();
        disposing.set(true);
        doDispose();
    }

    public UMOEndpointURI getEndpointURI()
    {
        return endpointUri;
    }

    protected UMOWorkManager getWorkManager()
    {
        return workManager;
    }

    protected void setWorkManager(UMOWorkManager workManager)
    {
        this.workManager = workManager;
    }

    public void connect() throws Exception
    {
        if (connected.get())
        {
            return;
        }

        if (connecting.compareAndSet(false, true))
        {
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
            connected.set(true);
            connecting.set(false);

            connector.fireNotification(new ConnectionNotification(this, getConnectEventId(),
                    ConnectionNotification.CONNECTION_CONNECTED));
        }
        catch (Exception e)
        {
            connected.set(false);
            connecting.set(false);

            connector.fireNotification(new ConnectionNotification(this, getConnectEventId(),
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

    public void disconnect() throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Disconnecting: " + this);
        }

        this.doDisconnect();
        connected.set(false);

        logger.info("Disconnected: " + this);

        connector.fireNotification(new ConnectionNotification(this, getConnectEventId(),
                ConnectionNotification.CONNECTION_DISCONNECTED));
    }

    public String getConnectionDescription()
    {
        return endpoint.getEndpointURI().toString();
    }

    public final void start() throws UMOException
    {
        if (stopped.compareAndSet(true, false))
        {
            if (!connected.get())
            {
                connectionStrategy.connect(this);
            }
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
            catch (UMOException e)
            {
                // TODO MULE-863: What should we really do?
                logger.error(e.getMessage(), e);
            }

        }
    }

    public final boolean isConnected()
    {
        return connected.get();
    }

    public InternalMessageListener getListener()
    {
        return listener;
    }

    public void setListener(InternalMessageListener listener)
    {
        this.listener = listener;
    }

    private class DefaultInternalMessageListener implements InternalMessageListener
    {

        public UMOMessage onMessage(UMOMessage message,
                                    UMOTransaction trans,
                                    boolean synchronous,
                                    OutputStream outputStream) throws UMOException
        {

            UMOMessage resultMessage = null;
            ResponseOutputStream ros = null;
            if (outputStream != null)
            {
                if (outputStream instanceof ResponseOutputStream)
                {
                    ros = (ResponseOutputStream) outputStream;
                }
                else
                {
                    ros = new ResponseOutputStream(outputStream);
                }
            }
            UMOSession session = new MuleSession(message, connector.getSessionHandler(), component);
            UMOEvent muleEvent = new MuleEvent(message, endpoint, session, synchronous, ros);
            RequestContext.setEvent(muleEvent);

            // Apply Security filter if one is set
            boolean authorised = false;
            if (endpoint.getSecurityFilter() != null)
            {
                try
                {
                    endpoint.getSecurityFilter().authenticate(muleEvent);
                    authorised = true;
                }
                catch (SecurityException e)
                {
                    // TODO MULE-863: Do we need to warn?
                    logger.warn("Request was made but was not authenticated: " + e.getMessage(), e);
                    connector.fireNotification(new SecurityNotification(e,
                            SecurityNotification.SECURITY_AUTHENTICATION_FAILED));
                    handleException(e);
                    resultMessage = message;
                    // setExceptionDetails(resultMessage, e);
                }
            }
            else
            {
                authorised = true;
            }

            if (authorised)
            {
                // the security filter may update the payload so we need to get the
                // latest event again
                muleEvent = RequestContext.getEvent();

                // This is a replyTo event for a current request
                if (UMOEndpoint.ENDPOINT_TYPE_RESPONSE.equals(endpoint.getType()))
                {
                    component.getDescriptor().getResponseRouter().route(muleEvent);
                    return null;
                }
                else
                {
                    resultMessage = component.getDescriptor().getInboundRouter().route(muleEvent);
                }
            }
            if (resultMessage != null)
            {
                RequestContext.rewriteEvent(resultMessage);
                if (resultMessage.getExceptionPayload() != null)
                {
                    setExceptionDetails(resultMessage, resultMessage.getExceptionPayload().getException());
                }
            }
            return applyResponseTransformer(resultMessage);
        }
    }

    protected String getConnectEventId()
    {
        return connector.getName() + ".receiver (" + endpoint.getEndpointURI() + ")";
    }

    protected UMOMessage applyResponseTransformer(UMOMessage returnMessage) throws TransformerException
    {
        UMOTransformer transformer = endpoint.getResponseTransformer();

        // no transformer, so do nothing.
        if (transformer == null)
        {
            return returnMessage;
        }

        if (returnMessage == null)
        {
            if (transformer.isAcceptNull())
            {
                returnMessage = new MuleMessage(NullPayload.getInstance(), RequestContext.getEventContext()
                        .getMessage());
            }
            else
            {
                return null;
            }
        }

        Object returnPayload = returnMessage.getPayload();
        if (transformer.isSourceTypeSupported(returnPayload.getClass()))
        {
            Object result = transformer.transform(returnPayload);
            if (result instanceof UMOMessage)
            {
                returnMessage = (UMOMessage) result;
            }
            else
            {
                // Try and wrap the response in the correct messageAdapter, if this
                // doesn't work for some reason
                // just use a standard adater
                // try {
                // UMOMessageAdapter adapter =
                // endpoint.getConnector().getMessageAdapter(result);
                // returnMessage = new MuleMessage(adapter, returnMessage);
                // } catch (MessagingException e) {
                // if(logger.isWarnEnabled()) {
                // logger.warn("Failed to wrap response in " +
                // endpoint.getConnector().getProtocol() + ". Error is: " +
                // e.getMessage());
                // }
                returnMessage = new MuleMessage(result, returnMessage);
                // }
                //
            }
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Response transformer: " + transformer + " doesn't support the result payload: "
                        + returnPayload.getClass());
            }
        }
        return returnMessage;
    }

    public void setReceiverKey(String receiverKey)
    {
        this.receiverKey = receiverKey;
    }

    public String getReceiverKey()
    {
        return receiverKey;
    }

    public String toString()
    {
        final StringBuffer sb = new StringBuffer(80);
        sb.append(ClassUtils.getSimpleName(this.getClass()));
        sb.append("{this=").append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(", receiverKey=").append(receiverKey);
        sb.append(", endpoint=").append(endpoint.getEndpointURI());
        sb.append('}');
        return sb.toString();
    }

    protected abstract void doStart() throws UMOException;

    protected abstract void doStop() throws UMOException;

    protected abstract void doConnect() throws Exception;

    protected abstract void doDisconnect() throws Exception;

    protected abstract void doDispose();

}
