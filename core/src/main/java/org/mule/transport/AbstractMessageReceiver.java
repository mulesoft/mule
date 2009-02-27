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

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleSession;
import org.mule.NullSessionHandler;
import org.mule.OptimizedRequestContext;
import org.mule.RequestContext;
import org.mule.ResponseOutputStream;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.SecurityException;
import org.mule.api.service.Service;
import org.mule.api.transaction.Transaction;
import org.mule.api.transport.Connector;
import org.mule.api.transport.InternalMessageListener;
import org.mule.api.transport.MessageReceiver;
import org.mule.config.ExceptionHelper;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.context.notification.SecurityNotification;
import org.mule.transaction.TransactionCoordination;
import org.mule.util.ClassUtils;
import org.mule.util.StringMessageUtils;

import java.io.OutputStream;

/**
 * <code>AbstractMessageReceiver</code> provides common methods for all Message
 * Receivers provided with Mule. A message receiver enables an endpoint to receive a
 * message from an external system.
 */
public abstract class AbstractMessageReceiver extends AbstractConnectable implements MessageReceiver
{
    /** The Service with which this receiver is associated with */
    protected Service service = null;

    private InternalMessageListener listener;

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
    private EndpointURI endpointUri;

    protected boolean responseEndpoint = false;

    /**
     * Creates the Message Receiver
     *
     * @param connector the endpoint that created this listener
     * @param service the service to associate with the receiver. When data is
     *                  received the service <code>dispatchEvent</code> or
     *                  <code>sendEvent</code> is used to dispatch the data to the
     *                  relevant Service.
     * @param endpoint  the provider contains the endpointUri on which the receiver
     *                  will listen on. The endpointUri can be anything and is specific to
     *                  the receiver implementation i.e. an email address, a directory, a
     *                  jms destination or port address.
     * @see Service
     * @see InboundEndpoint
     */
    public AbstractMessageReceiver(Connector connector, Service service, InboundEndpoint endpoint) throws CreateException
    {
        super(endpoint);
        
        setService(service);
        if (service.getResponseRouter() != null && service.getResponseRouter().getEndpoints().contains(endpoint))
        {
            responseEndpoint = true;
        }
    }

    /**
     * Method used to perform any initialisation work. If a fatal error occurs during
     * initialisation an <code>InitialisationException</code> should be thrown,
     * causing the Mule instance to shutdown. If the error is recoverable, say by
     * retrying to connect, a <code>RecoverableException</code> should be thrown.
     * There is no guarantee that by throwing a Recoverable exception that the Mule
     * instance will not shut down.
     *
     * @throws org.mule.api.lifecycle.InitialisationException
     *          if a fatal error occurs causing the Mule
     *          instance to shutdown
     * @throws org.mule.api.lifecycle.RecoverableException
     *          if an error occurs that can be recovered from
     */
    @Override
    public final void initialise() throws InitialisationException
    {
        super.initialise();
        
        listener = new DefaultInternalMessageListener();
        endpointUri = endpoint.getEndpointURI();

        doInitialise();
    }

    @Override
    public final synchronized void dispose()
    {
        super.dispose();
        try
        {
            doDispose();
        }
        finally
        {
            disposed.set(true);
        }
    }

    /**
     * This method is used to set any additional aand possibly transport specific
     * information on the return message where it has an exception payload.
     *
     * @param message
     * @param exception
     */
    protected void setExceptionDetails(MuleMessage message, Throwable exception)
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

    public Service getService()
    {
        return service;
    }

    public final MuleMessage routeMessage(MuleMessage message) throws MuleException
    {
        return routeMessage(message, (endpoint.isSynchronous() || TransactionCoordination.getInstance()
                .getTransaction() != null));
    }

    public final MuleMessage routeMessage(MuleMessage message, boolean synchronous) throws MuleException
    {
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        return routeMessage(message, tx, tx != null || synchronous, null);
    }

    public final MuleMessage routeMessage(MuleMessage message, Transaction trans, boolean synchronous)
            throws MuleException
    {
        return routeMessage(message, trans, synchronous, null);
    }

    public final MuleMessage routeMessage(MuleMessage message, OutputStream outputStream) throws MuleException
    {
        return routeMessage(message, endpoint.isSynchronous(), outputStream);
    }

    public final MuleMessage routeMessage(MuleMessage message, boolean synchronous, OutputStream outputStream)
            throws MuleException
    {
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        return routeMessage(message, tx, tx != null || synchronous, outputStream);
    }

    public final MuleMessage routeMessage(MuleMessage message,
                                         Transaction trans,
                                         boolean synchronous,
                                         OutputStream outputStream) throws MuleException
    {

        if (connector.isEnableMessageEvents())
        {
            connector.fireNotification(
                    new EndpointMessageNotification(message, endpoint, service.getName(), EndpointMessageNotification.MESSAGE_RECEIVED));
        }

        //If synchronous set on the endpoint, we need to set it on the message
        if (endpoint.isSynchronous())
        {
            message.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, true);
        }
        //Enforce a sync endpoint if remote sync is set
        if (message.getBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, false))
        {
            synchronous = true;
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
                        + StringMessageUtils.truncate(StringMessageUtils.toString(message.getPayload()), 200, false));
                logger.trace("Message detail: \n" + message);
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
                message = handleUnacceptedFilter(message);
                RequestContext.setEvent(new DefaultMuleEvent(message, endpoint,
                        new DefaultMuleSession(message, new NullSessionHandler(), connector.getMuleContext()), synchronous));
                return message;
            }
        }
        return listener.onMessage(message, trans, synchronous, outputStream);
    }

    protected MuleMessage handleUnacceptedFilter(MuleMessage message)
    {
        String messageId;
        messageId = message.getUniqueId();

        if (logger.isDebugEnabled())
        {
            logger.debug("Message " + messageId + " failed to pass filter on endpoint: " + endpoint
                    + ". Message is being ignored");
        }

        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.api.transport.MessageReceiver#setSession(org.mule.api.MuleSession)
     */
    public void setService(Service service)
    {
        if (service == null)
        {
            throw new IllegalArgumentException("Service cannot be null");
        }
        this.service = service;
    }

    public EndpointURI getEndpointURI()
    {
        return endpointUri;
    }

    public String getConnectionDescription()
    {
        return endpoint.getEndpointURI().toString();
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

        public MuleMessage onMessage(MuleMessage message,
                                    Transaction trans,
                                    boolean synchronous,
                                    OutputStream outputStream) throws MuleException
        {

            MuleMessage resultMessage = null;
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
            MuleSession session = new DefaultMuleSession(message, connector.getSessionHandler(), service, connector.getMuleContext());
            MuleEvent muleEvent = new DefaultMuleEvent(message, endpoint, session, synchronous, ros);
            muleEvent = OptimizedRequestContext.unsafeSetEvent(muleEvent);

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
                    logger.warn("Request was made but was not authenticated: " + e.getMessage(), e);
                    connector.fireNotification(new SecurityNotification(e,
                            SecurityNotification.SECURITY_AUTHENTICATION_FAILED));
                    handleException(e);
                    resultMessage = RequestContext.getEvent().getMessage();
                }
            }
            else
            {
                authorised = true;
            }

            if (authorised)
            {
                // This is a replyTo event for a current request
                if (responseEndpoint)
                {
                    // Transform response message before it is processed by response router(s)
                    muleEvent.transformMessage();
                    service.getResponseRouter().route(muleEvent);
                    return null;
                }
                else
                {
                    resultMessage = service.getInboundRouter().route(muleEvent);
                }
            }
            if (resultMessage != null)
            {
                if (resultMessage.getExceptionPayload() != null)
                {
                    setExceptionDetails(resultMessage, resultMessage.getExceptionPayload().getException());
                }
                resultMessage.applyTransformers(endpoint.getResponseTransformers());
            }
            return resultMessage;
        }
    }

    protected String getConnectEventId()
    {
        return connector.getName() + ".receiver (" + endpoint.getEndpointURI() + ")";
    }

    public void setReceiverKey(String receiverKey)
    {
        this.receiverKey = receiverKey;
    }

    public String getReceiverKey()
    {
        return receiverKey;
    }

    public InboundEndpoint getEndpoint()
    {
        return (InboundEndpoint) super.getEndpoint();
    }
    
    public void setEndpoint(InboundEndpoint endpoint)
    {
        super.setEndpoint(endpoint);
    }
    
    @Override
    protected WorkManager getWorkManager()
    {
        try
        {
            return connector.getReceiverWorkManager("receiver");
        }
        catch (MuleException e)
        {
            logger.error(e);
            return null;
        }
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
}
