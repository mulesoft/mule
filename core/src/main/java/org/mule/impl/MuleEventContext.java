/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.RegistryContext;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.FutureMessageResult;
import org.mule.umo.TransactionException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.transformer.TransformerException;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;

import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleEventContext</code> is the context object for the current request.
 * Using the context, developers can send/dispatch/receive events programmatically as
 * well as manage transactions.
 */
public class MuleEventContext implements UMOEventContext
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(MuleEventContext.class);

    private final UMOEvent event;
    private final UMOSession session;

    public MuleEventContext(UMOEvent event)
    {
        this.event = event;
        this.session = event.getSession();
    }

    /**
     * Returns the message payload for this event
     * 
     * @return the message payload for this event
     */
    public UMOMessage getMessage()
    {
        return event.getMessage();
    }

    /**
     * Reterns the conents of the message as a byte array.
     * 
     * @return the conents of the message as a byte array
     * @throws org.mule.umo.UMOException if the message cannot be converted into an
     *             array of bytes
     */
    public byte[] getMessageAsBytes() throws UMOException
    {
        return event.getMessageAsBytes();
    }

    /**
     * Returns the message transformed into it's recognised or expected format. The
     * transformer used is the one configured on the endpoint through which this
     * event was received.
     * 
     * @return the message transformed into it's recognised or expected format.
     * @throws org.mule.umo.transformer.TransformerException if a failure occurs in
     *             the transformer
     * @see org.mule.umo.transformer.UMOTransformer
     */
    public Object getTransformedMessage() throws TransformerException
    {
        return event.getTransformedMessage();
    }

    /**
     * Returns the message transformed into its recognised or expected format. The
     * transformer used is the one configured on the endpoint through which this
     * event was received.
     * 
     * @param expectedType The class type required for the return object. This param
     *            just provides a convienient way to manage type casting of
     *            transformed objects
     * @return the message transformed into it's recognised or expected format.
     * @throws org.mule.umo.transformer.TransformerException if a failure occurs or
     *             if the return type is not the same as the expected type in the
     *             transformer
     * @see org.mule.umo.transformer.UMOTransformer
     */
    public Object getTransformedMessage(Class expectedType) throws TransformerException
    {
        Object message = getTransformedMessage();
        if (expectedType != null && expectedType.isAssignableFrom(message.getClass()))
        {
            return message;
        }
        else
        {
            throw new TransformerException(
                CoreMessages.transformOnObjectNotOfSpecifiedType(this.getComponent().getName(), expectedType),
                    this.event.getEndpoint().getTransformers());
        }
    }

    /**
     * Returns the message transformed into it's recognised or expected format and
     * then into an array of bytes. The transformer used is the one configured on the
     * endpoint through which this event was received.
     * 
     * @return the message transformed into it's recognised or expected format as an
     *         array of bytes.
     * @throws org.mule.umo.transformer.TransformerException if a failure occurs in
     *             the transformer
     * @see org.mule.umo.transformer.UMOTransformer
     */
    public byte[] getTransformedMessageAsBytes() throws TransformerException
    {
        return event.getTransformedMessageAsBytes();
    }

    /**
     * Returns the message transformed into it's recognised or expected format and
     * then into a String. The transformer used is the one configured on the endpoint
     * through which this event was received.
     * 
     * @return the message transformed into it's recognised or expected format as a
     *         Strings.
     * @throws org.mule.umo.transformer.TransformerException if a failure occurs in
     *             the transformer
     * @see org.mule.umo.transformer.UMOTransformer
     */
    public String getTransformedMessageAsString(String encoding) throws TransformerException
    {
        return event.getTransformedMessageAsString(encoding);
    }

    /**
     * Returns the message contents as a string
     * 
     * @return the message contents as a string
     * @throws org.mule.umo.UMOException if the message cannot be converted into a
     *             string
     */
    public String getMessageAsString(String encoding) throws UMOException
    {
        return event.getMessageAsString(encoding);
    }

    /**
     * Returns the message transformed into it's recognised or expected format and
     * then into a String. The transformer used is the one configured on the endpoint
     * through which this event was received. This method will use the default
     * encoding on the event
     * 
     * @return the message transformed into it's recognised or expected format as a
     *         Strings.
     * @throws org.mule.umo.transformer.TransformerException if a failure occurs in
     *             the transformer
     * @see org.mule.umo.transformer.UMOTransformer
     */
    public String getTransformedMessageAsString() throws TransformerException
    {
        return event.getTransformedMessageAsString();
    }

    /**
     * Returns the message contents as a string This method will use the default
     * encoding on the event
     * 
     * @return the message contents as a string
     * @throws org.mule.umo.UMOException if the message cannot be converted into a
     *             string
     */
    public String getMessageAsString() throws UMOException
    {
        return event.getMessageAsString();
    }

    /**
     * Returns the current transaction (if any) for the session
     * 
     * @return the current transaction for the session or null if there is no
     *         transaction in progress
     */
    public UMOTransaction getCurrentTransaction()
    {
        return TransactionCoordination.getInstance().getTransaction();
    }

    public void markTransactionForRollback() throws TransactionException
    {
        if (getCurrentTransaction() != null)
        {
            getCurrentTransaction().setRollbackOnly();
        }
    }

    /**
     * This will send an event via the configured outbound router on the component
     * 
     * @param message the message to send
     * @return the result of the send if any
     * @throws org.mule.umo.UMOException if there is no outbound endpoint configured
     *             on the component or the events fails during dispatch
     */
    public UMOMessage sendEvent(Object message) throws UMOException
    {
        return sendEvent(new MuleMessage(message, event.getMessage()));
    }

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule UMO in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpoint The endpoint to disptch the event through.
     * @return the return Message from the call or null if there was no result
     * @throws org.mule.umo.UMOException if the event fails to be processed by the
     *             component or the transport for the endpoint
     */
    public UMOMessage sendEvent(UMOMessage message, UMOImmutableEndpoint endpoint) throws UMOException
    {
        // If synchronous receive has not been explicitly set, default it to true
        setRemoteSync(message, endpoint);
        return session.sendEvent(message, endpoint);
    }

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule UMO in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the message payload to send
     * @return the return Message from the call or null if there was no result
     * @throws org.mule.umo.UMOException if the event fails to be processed by the
     *             component or the transport for the endpoint
     */
    public UMOMessage sendEvent(UMOMessage message) throws UMOException
    {
        // If synchronous receive has not been explicitly set, default it to
        // true
        setRemoteSync(message, event.getEndpoint());
        return session.sendEvent(message);
    }

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule UMO in the pool or via the
     * endpointUri configured for the event
     * 
     * @param message the event message payload to send
     * @param endpointUri The endpointUri to disptch the event through
     * @return the return Message from the call or null if there was no result
     * @throws org.mule.umo.UMOException if the event fails to be processed by the
     *             component or the transport for the endpointUri
     */
    public UMOMessage sendEvent(UMOMessage message, UMOEndpointURI endpointUri) throws UMOException
    {
        UMOImmutableEndpoint endpoint = getManagementContext().getRegistry().lookupEndpointFactory().getEndpoint(
            endpointUri, UMOEndpoint.ENDPOINT_TYPE_SENDER, getManagementContext());

        // If synchronous receive has not been explicitly set, default it to
        // true
        setRemoteSync(message, endpoint);
        return session.sendEvent(message, endpoint);
    }

    /**
     * sends an event request via the configured outbound router for this component.
     * This method return immediately, but the result of the event invocation
     * available from the returned a Future result that can be accessed later by the
     * the returned FutureMessageResult. the Future messageResult can be queried at
     * any time to check that the invocation has completed. A timeout is associated
     * with the invocation, which is the maximum time in milli-seconds that the
     * invocation should take to complete
     * 
     * @param message the object that is the payload of the event
     * @param timeout how long to block in milliseconds waiting for a result
     * @return the result message if any of the invocation
     * @throws org.mule.umo.UMOException if the dispatch fails or the components or
     *             transfromers cannot be found
     * @see org.mule.umo.FutureMessageResult
     */
    public FutureMessageResult sendEventAsync(final Object message, final int timeout) throws UMOException
    {
        Callable callable = new Callable()
        {
            public Object call() throws Exception
            {
                UMOMessage umoMessage = new MuleMessage(message, event.getMessage());
                umoMessage.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, true);
                umoMessage.setIntProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, timeout);
                return sendEvent(umoMessage);
            }
        };

        FutureMessageResult result = new FutureMessageResult(callable);
        // TODO MULE-732: use injected ExecutorService
        result.execute();
        return result;
    }

    /**
     * sends an event request via the configured outbound router for this component.
     * This method return immediately, but the result of the event invocation
     * available from the returned a Future result that can be accessed later by the
     * the returned FutureMessageResult. the Future messageResult can be queried at
     * any time to check that the invocation has completed. A timeout is associated
     * with the invocation, which is the maximum time in milli-seconds that the
     * invocation should take to complete
     * 
     * @param message the UMOMessage of the event
     * @param timeout how long to block in milliseconds waiting for a result
     * @return the result message if any of the invocation
     * @throws org.mule.umo.UMOException if the dispatch fails or the components or
     *             transfromers cannot be found
     * @see org.mule.umo.FutureMessageResult
     */
    public FutureMessageResult sendEventAsync(final UMOMessage message, final int timeout)
        throws UMOException
    {
        Callable callable = new Callable()
        {
            public Object call() throws Exception
            {
                message.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, true);
                message.setIntProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, timeout);
                return sendEvent(message);
            }
        };

        FutureMessageResult result = new FutureMessageResult(callable);
        // TODO MULE-732: use injected ExecutorService
        result.execute();
        return result;
    }

    /**
     * sends an event request via the configured outbound router for this component.
     * This method return immediately, but the result of the event invocation
     * available from the returned a Future result that can be accessed later by the
     * the returned FutureMessageResult. the Future messageResult can be queried at
     * any time to check that the invocation has completed. A timeout is associated
     * with the invocation, which is the maximum time in milli-seconds that the
     * invocation should take to complete
     * 
     * @param message the UMOMessage of the event
     * @param endpointUri the endpointUri to dispatch to
     * @param timeout how long to block in milliseconds waiting for a result
     * @return the result message if any of the invocation
     * @throws org.mule.umo.UMOException if the dispatch fails or the components or
     *             transfromers cannot be found
     * @see org.mule.umo.FutureMessageResult
     */
    public FutureMessageResult sendEventAsync(final UMOMessage message,
                                              final UMOEndpointURI endpointUri,
                                              final int timeout) throws UMOException
    {
        Callable callable = new Callable()
        {
            public Object call() throws Exception
            {
                message.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, true);
                message.setIntProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, timeout);
                return sendEvent(message, endpointUri);
            }
        };

        FutureMessageResult result = new FutureMessageResult(callable);
        // TODO MULE-732: use injected ExecutorService
        result.execute();
        return result;
    }

    /**
     * sends an event request via the configured outbound router for this component.
     * This method return immediately, but the result of the event invocation
     * available from the returned a Future result that can be accessed later by the
     * the returned FutureMessageResult. the Future messageResult can be queried at
     * any time to check that the invocation has completed. A timeout is associated
     * with the invocation, which is the maximum time in milli-seconds that the
     * invocation should take to complete
     * 
     * @param message the UMOMessage of the event
     * @param endpointName The endpoint name to disptch the event through. This will
     *            be looked up first on the component configuration and then on the
     *            mule manager configuration
     * @param timeout how long to block in milliseconds waiting for a result
     * @return the result message if any of the invocation
     * @throws org.mule.umo.UMOException if the dispatch fails or the components or
     *             transfromers cannot be found
     * @see org.mule.umo.FutureMessageResult
     */
    public FutureMessageResult sendEventAsync(final UMOMessage message,
                                              final String endpointName,
                                              final int timeout) throws UMOException
    {
        Callable callable = new Callable()
        {
            public Object call() throws Exception
            {
                message.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, true);
                message.setIntProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, timeout);
                return sendEvent(message, endpointName);
            }
        };

        FutureMessageResult result = new FutureMessageResult(callable);
        // TODO MULE-732: use injected ExecutorService
        result.execute();
        return result;
    }

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule UMO in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpointName The endpoint name to disptch the event through. This will
     *            be looked up first on the component configuration and then on the
     *            mule manager configuration
     * @return the return Message from the call or null if there was no result
     * @throws org.mule.umo.UMOException if the event fails to be processed by the
     *             component or the transport for the endpoint
     */
    public UMOMessage sendEvent(UMOMessage message, String endpointName) throws UMOException
    {
        UMOImmutableEndpoint endpoint = RegistryContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            endpointName, getManagementContext());
        setRemoteSync(message, endpoint);
        return session.sendEvent(message, endpoint);
    }

    /**
     * This will dispatch an event asynchronously via the configured outbound
     * endpoint on the component for this session
     * 
     * @param message payload to dispatch
     * @throws org.mule.umo.UMOException if there is no outbound endpoint configured
     *             on the component or the events fails during dispatch
     */
    public void dispatchEvent(Object message) throws UMOException
    {
        session.dispatchEvent(new MuleMessage(message, event.getMessage()));
    }

    /**
     * This will dispatch an event asynchronously via the configured outbound
     * endpoint on the component for this session
     * 
     * @param message the message to send
     * @throws org.mule.umo.UMOException if there is no outbound endpoint configured
     *             on the component or the events fails during dispatch
     */
    public void dispatchEvent(UMOMessage message) throws UMOException
    {
        session.dispatchEvent(message);
    }

    /**
     * Depending on the session state this methods either Passes an event
     * asynchronously to the next available Mule UMO in the pool or via the
     * endpointUri configured for the event
     * 
     * @param message the event message payload to send
     * @param endpointUri the endpointUri to dispatc the event to first on the
     *            component configuration and then on the mule manager configuration
     * @throws org.mule.umo.UMOException if the event fails to be processed by the
     *             component or the transport for the endpointUri
     */
    public void dispatchEvent(UMOMessage message, UMOEndpointURI endpointUri) throws UMOException
    {
        UMOImmutableEndpoint endpoint = getManagementContext().getRegistry().lookupEndpointFactory().getEndpoint(
            endpointUri, UMOEndpoint.ENDPOINT_TYPE_SENDER, getManagementContext());
        session.dispatchEvent(message, endpoint);
    }

    /**
     * Depending on the session state this methods either Passes an event
     * asynchronously to the next available Mule UMO in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpointName The endpoint name to disptch the event through. This will
     *            be looked up first on the component configuration and then on the
     *            mule manager configuration
     * @throws org.mule.umo.UMOException if the event fails to be processed by the
     *             component or the transport for the endpoint
     */
    public void dispatchEvent(UMOMessage message, String endpointName) throws UMOException
    {
        session.dispatchEvent(message, endpointName);
    }

    /**
     * Depending on the session state this methods either Passes an event
     * asynchronously to the next available Mule UMO in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpoint The endpoint name to disptch the event through.
     * @throws org.mule.umo.UMOException if the event fails to be processed by the
     *             component or the transport for the endpoint
     */
    public void dispatchEvent(UMOMessage message, UMOImmutableEndpoint endpoint) throws UMOException
    {
        session.dispatchEvent(message, endpoint);
    }

    /**
     * Requests a synchronous receive of an event on the component
     * 
     * @param endpoint the endpoint identifing the endpointUri on ewhich the event
     *            will be received
     * @param timeout time in milliseconds before the request timesout
     * @return The requested event or null if the request times out
     * @throws org.mule.umo.UMOException if the request operation fails
     */
    public UMOMessage receiveEvent(UMOImmutableEndpoint endpoint, long timeout) throws UMOException
    {
        return session.receiveEvent(endpoint, timeout);
    }

    /**
     * Requests a synchronous receive of an event on the component
     * 
     * @param endpointName the endpoint identifing the endpointUri on ewhich the
     *            event will be received
     * @param timeout time in milliseconds before the request timesout
     * @return The requested event or null if the request times out
     * @throws org.mule.umo.UMOException if the request operation fails
     */
    public UMOMessage receiveEvent(String endpointName, long timeout) throws UMOException
    {
        return session.receiveEvent(endpointName, timeout);
    }

    /**
     * Requests a synchronous receive of an event on the component
     * 
     * @param endpointUri the endpointUri on which the event will be received
     * @param timeout time in milliseconds before the request timesout
     * @return The requested event or null if the request times out
     * @throws org.mule.umo.UMOException if the request operation fails
     */
    public UMOMessage receiveEvent(UMOEndpointURI endpointUri, long timeout) throws UMOException
    {
        UMOImmutableEndpoint endpoint = getManagementContext().getRegistry().lookupEndpointFactory().getEndpoint(
            endpointUri, UMOEndpoint.ENDPOINT_TYPE_SENDER, getManagementContext());
        return session.receiveEvent(endpoint, timeout);
    }

    /**
     * @return the component descriptor of the component that received this event
     */
    public UMOComponent getComponent()
    {
        return event.getComponent();
    }

    /**
     * Determines whether the default processing for this event will be executed. By
     * default, the Mule server will route events according to a components
     * configuration. The user can override this behaviour by obtaining a reference
     * to the Event context, either by implementing
     * <code>org.mule.umo.lifecycle.Callable</code> or calling
     * <code>RequestContext.getEventContext</code> to obtain the UMOEventContext for
     * the current thread. The user can programmatically control how events are
     * dispatched.
     * 
     * @return Returns true is the user has set stopFurtherProcessing.
     * @see org.mule.umo.manager.UMOManager
     * @see org.mule.umo.UMOEventContext
     * @see org.mule.umo.lifecycle.Callable
     */
    public boolean isStopFurtherProcessing()
    {
        return RequestContext.getEvent().isStopFurtherProcessing();
    }

    /**
     * Determines whether the default processing for this event will be executed. By
     * default, the Mule server will route events according to a components
     * configuration. The user can override this behaviour by obtaining a reference
     * to the Event context, either by implementing
     * <code>org.mule.umo.lifecycle.Callable</code> or calling
     * <code>UMOManager.getEventContext</code> to obtain the UMOEventContext for
     * the current thread. The user can programmatically control how events are
     * dispached.
     * 
     * @param stopFurtherProcessing the value to set.
     */
    public void setStopFurtherProcessing(boolean stopFurtherProcessing)
    {
        event.setStopFurtherProcessing(stopFurtherProcessing);
    }

    /**
     * An outputstream the can optionally be used write response data to an incoming
     * message.
     * 
     * @return an output stream if one has been made available by the message receiver
     *         that received the message
     */
    public OutputStream getOutputStream()
    {
        return event.getOutputStream();
    }

    /**
     * Determines whether the was sent synchrounously or not
     * 
     * @return true if the event is synchronous
     */
    public boolean isSynchronous()
    {
        return event.isSynchronous();
    }

    public UMOEndpointURI getEndpointURI()
    {
        return event.getEndpoint().getEndpointURI();
    }

    /**
     * Returns the transaction for the current event or null if there is no
     * transaction in progresss
     * 
     * @return the transaction for the current event or null if there is no
     *         transaction in progresss
     */
    public UMOTransaction getTransaction()
    {
        return TransactionCoordination.getInstance().getTransaction();
    }

    /**
     * Get the timeout value associated with the event
     * 
     * @return the timeout for the event
     */
    public int getTimeout()
    {
        return event.getTimeout();
    }

    private void setRemoteSync(UMOMessage message, UMOImmutableEndpoint endpoint)
    {
        if (endpoint.isRemoteSync())
        {
            if (getTransaction() == null)
            {
                message.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, true);
            }
            else
            {
                throw new IllegalStateException(
                    CoreMessages.cannotUseTxAndRemoteSync().getMessage());
            }
        }
    }

    /**
     * Determines whether the event flow is being streamed
     * 
     * @return true if the request should be streamed
     */
    public boolean isStreaming()
    {
        return event.getEndpoint().isStreaming();
    }

    /**
     * Gets the encoding for the current message. For potocols that send encoding
     * Information with the message, this method should be overriden to expose the
     * transport encoding, otherwise the default encoding in the Mule configuration
     * will be used
     * 
     * @return the encoding for this message. This method must never return null
     */
    public String getEncoding()
    {
        return event.getEncoding();
    }

    public UMOSession getSession()
    {
        return event.getSession();
    }

    public String toString()
    {
        return event.toString();
    }

    public UMOManagementContext getManagementContext()
    {
        return event.getManagementContext();
    }
}
