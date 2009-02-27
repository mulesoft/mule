/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.FutureMessageResult;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transformer.TransformerException;
import org.mule.transaction.TransactionCoordination;

import java.io.OutputStream;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DefaultMuleEventContext</code> is the context object for the current request.
 * Using the context, developers can send/dispatch/receive events programmatically as
 * well as manage transactions.
 */
public class DefaultMuleEventContext implements MuleEventContext
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(DefaultMuleEventContext.class);

    private final MuleEvent event;
    private final MuleSession session;

    public DefaultMuleEventContext(MuleEvent event)
    {
        this.event = event;
        this.session = event.getSession();
    }

    /**
     * Returns the message payload for this event
     * 
     * @return the message payload for this event
     */
    public MuleMessage getMessage()
    {
        return event.getMessage();
    }

    /**
     * Reterns the conents of the message as a byte array.
     * 
     * @return the conents of the message as a byte array
     * @throws org.mule.api.MuleException if the message cannot be converted into an
     *             array of bytes
     */
    public byte[] getMessageAsBytes() throws MuleException
    {
        return event.getMessageAsBytes();
    }

    /**
     * Returns the message transformed into it's recognised or expected format. The
     * transformer used is the one configured on the endpoint through which this
     * event was received.
     * 
     * @return the message transformed into it's recognised or expected format.
     * @throws org.mule.api.transformer.TransformerException if a failure occurs in
     *             the transformer
     * @see org.mule.api.transformer.Transformer
     */
    public Object transformMessage() throws TransformerException
    {
        return event.transformMessage();
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
     * @throws org.mule.api.transformer.TransformerException if a failure occurs or
     *             if the return type is not the same as the expected type in the
     *             transformer
     * @see org.mule.api.transformer.Transformer
     */
    public Object transformMessage(Class expectedType) throws TransformerException
    {
        return event.transformMessage(expectedType);
    }

    /**
     * Returns the message transformed into it's recognised or expected format and
     * then into an array of bytes. The transformer used is the one configured on the
     * endpoint through which this event was received.
     * 
     * @return the message transformed into it's recognised or expected format as an
     *         array of bytes.
     * @throws org.mule.api.transformer.TransformerException if a failure occurs in
     *             the transformer
     * @see org.mule.api.transformer.Transformer
     */
    public byte[] transformMessageToBytes() throws TransformerException
    {
        return event.transformMessageToBytes();
    }

    /**
     * Returns the message contents as a string
     * 
     * @return the message contents as a string
     * @throws org.mule.api.MuleException if the message cannot be converted into a
     *             string
     */
    public String getMessageAsString(String encoding) throws MuleException
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
     * @throws org.mule.api.transformer.TransformerException if a failure occurs in
     *             the transformer
     * @see org.mule.api.transformer.Transformer
     */
    public String transformMessageToString() throws TransformerException
    {
        return event.transformMessageToString();
    }

    /**
     * Returns the message contents as a string This method will use the default
     * encoding on the event
     * 
     * @return the message contents as a string
     * @throws org.mule.api.MuleException if the message cannot be converted into a
     *             string
     */
    public String getMessageAsString() throws MuleException
    {
        return event.getMessageAsString();
    }

    /**
     * Returns the current transaction (if any) for the session
     * 
     * @return the current transaction for the session or null if there is no
     *         transaction in progress
     */
    public Transaction getCurrentTransaction()
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
     * This will send an event via the configured outbound router on the service
     * 
     * @param message the message to send
     * @return the result of the send if any
     * @throws org.mule.api.MuleException if there is no outbound endpoint configured
     *             on the service or the events fails during dispatch
     */
    public MuleMessage sendEvent(Object message) throws MuleException
    {
        return sendEvent(new DefaultMuleMessage(message, event.getMessage()));
    }

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule component in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpoint The endpoint to disptch the event through.
     * @return the return Message from the call or null if there was no result
     * @throws org.mule.api.MuleException if the event fails to be processed by the
     *             service or the transport for the endpoint
     */
    public MuleMessage sendEvent(MuleMessage message, OutboundEndpoint endpoint) throws MuleException
    {
        // If synchronous receive has not been explicitly set, default it to true
        return session.sendEvent(message, endpoint);
    }

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule component in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the message payload to send
     * @return the return Message from the call or null if there was no result
     * @throws org.mule.api.MuleException if the event fails to be processed by the
     *             service or the transport for the endpoint
     */
    public MuleMessage sendEvent(MuleMessage message) throws MuleException
    {
        // If synchronous receive has not been explicitly set, default it to
        // true
        return session.sendEvent(message);
    }

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule component in the pool or via the
     * endpointUri configured for the event
     * 
     * @param message the event message payload to send
     * @param endpointUri The endpointUri to disptch the event through
     * @return the return Message from the call or null if there was no result
     * @throws org.mule.api.MuleException if the event fails to be processed by the
     *             service or the transport for the endpointUri
     */
    public MuleMessage sendEvent(MuleMessage message, EndpointURI endpointUri) throws MuleException
    {
        OutboundEndpoint endpoint =
                getMuleContext().getRegistry().lookupEndpointFactory().getOutboundEndpoint(endpointUri);

        // If synchronous receive has not been explicitly set, default it to
        // true
        return session.sendEvent(message, endpoint);
    }

    /**
     * sends an event request via the configured outbound router for this service.
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
     * @throws org.mule.api.MuleException if the dispatch fails or the components or
     *             transfromers cannot be found
     * @see org.mule.api.FutureMessageResult
     */
    public FutureMessageResult sendEventAsync(final Object message, final int timeout) throws MuleException
    {
        Callable callable = new Callable()
        {
            public Object call() throws Exception
            {
                MuleMessage muleMessage = new DefaultMuleMessage(message, event.getMessage());
                muleMessage.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, true);
                muleMessage.setIntProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, timeout);
                return sendEvent(muleMessage);
            }
        };

        FutureMessageResult result = new FutureMessageResult(callable);
        // TODO MULE-732: use injected ExecutorService
        result.execute();
        return result;
    }

    /**
     * sends an event request via the configured outbound router for this service.
     * This method return immediately, but the result of the event invocation
     * available from the returned a Future result that can be accessed later by the
     * the returned FutureMessageResult. the Future messageResult can be queried at
     * any time to check that the invocation has completed. A timeout is associated
     * with the invocation, which is the maximum time in milli-seconds that the
     * invocation should take to complete
     * 
     * @param message the MuleMessage of the event
     * @param timeout how long to block in milliseconds waiting for a result
     * @return the result message if any of the invocation
     * @throws org.mule.api.MuleException if the dispatch fails or the components or
     *             transfromers cannot be found
     * @see org.mule.api.FutureMessageResult
     */
    public FutureMessageResult sendEventAsync(final MuleMessage message, final int timeout)
        throws MuleException
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
     * sends an event request via the configured outbound router for this service.
     * This method return immediately, but the result of the event invocation
     * available from the returned a Future result that can be accessed later by the
     * the returned FutureMessageResult. the Future messageResult can be queried at
     * any time to check that the invocation has completed. A timeout is associated
     * with the invocation, which is the maximum time in milli-seconds that the
     * invocation should take to complete
     * 
     * @param message the MuleMessage of the event
     * @param endpointUri the endpointUri to dispatch to
     * @param timeout how long to block in milliseconds waiting for a result
     * @return the result message if any of the invocation
     * @throws org.mule.api.MuleException if the dispatch fails or the components or
     *             transfromers cannot be found
     * @see org.mule.api.FutureMessageResult
     */
    public FutureMessageResult sendEventAsync(final MuleMessage message,
                                              final EndpointURI endpointUri,
                                              final int timeout) throws MuleException
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
     * sends an event request via the configured outbound router for this service.
     * This method return immediately, but the result of the event invocation
     * available from the returned a Future result that can be accessed later by the
     * the returned FutureMessageResult. the Future messageResult can be queried at
     * any time to check that the invocation has completed. A timeout is associated
     * with the invocation, which is the maximum time in milli-seconds that the
     * invocation should take to complete
     * 
     * @param message the MuleMessage of the event
     * @param endpointName The endpoint name to disptch the event through. This will
     *            be looked up first on the service configuration and then on the
     *            mule manager configuration
     * @param timeout how long to block in milliseconds waiting for a result
     * @return the result message if any of the invocation
     * @throws org.mule.api.MuleException if the dispatch fails or the components or
     *             transfromers cannot be found
     * @see org.mule.api.FutureMessageResult
     */
    public FutureMessageResult sendEventAsync(final MuleMessage message,
                                              final String endpointName,
                                              final int timeout) throws MuleException
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
     * synchronously to the next available Mule component in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpointName The endpoint name to disptch the event through. This will
     *            be looked up first on the service configuration and then on the
     *            mule manager configuration
     * @return the return Message from the call or null if there was no result
     * @throws org.mule.api.MuleException if the event fails to be processed by the
     *             service or the transport for the endpoint
     */
    public MuleMessage sendEvent(MuleMessage message, String endpointName) throws MuleException
    {
        OutboundEndpoint endpoint = getMuleContext().getRegistry().lookupEndpointFactory().getOutboundEndpoint(endpointName);
        return session.sendEvent(message, endpoint);
    }

    /**
     * This will dispatch an event asynchronously via the configured outbound
     * endpoint on the service for this session
     * 
     * @param message payload to dispatch
     * @throws org.mule.api.MuleException if there is no outbound endpoint configured
     *             on the service or the events fails during dispatch
     */
    public void dispatchEvent(Object message) throws MuleException
    {
        session.dispatchEvent(new DefaultMuleMessage(message, event.getMessage()));
    }

    /**
     * This will dispatch an event asynchronously via the configured outbound
     * endpoint on the service for this session
     * 
     * @param message the message to send
     * @throws org.mule.api.MuleException if there is no outbound endpoint configured
     *             on the service or the events fails during dispatch
     */
    public void dispatchEvent(MuleMessage message) throws MuleException
    {
        session.dispatchEvent(message);
    }

    /**
     * Depending on the session state this methods either Passes an event
     * asynchronously to the next available Mule component in the pool or via the
     * endpointUri configured for the event
     * 
     * @param message the event message payload to send
     * @param endpointUri the endpointUri to dispatc the event to first on the
     *            service configuration and then on the mule manager configuration
     * @throws org.mule.api.MuleException if the event fails to be processed by the
     *             service or the transport for the endpointUri
     */
    public void dispatchEvent(MuleMessage message, EndpointURI endpointUri) throws MuleException
    {
        OutboundEndpoint endpoint =
                getMuleContext().getRegistry().lookupEndpointFactory().getOutboundEndpoint(endpointUri);
        session.dispatchEvent(message, endpoint);
    }

    /**
     * Depending on the session state this methods either Passes an event
     * asynchronously to the next available Mule component in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpointName The endpoint name to disptch the event through. This will
     *            be looked up first on the service configuration and then on the
     *            mule manager configuration
     * @throws org.mule.api.MuleException if the event fails to be processed by the
     *             service or the transport for the endpoint
     */
    public void dispatchEvent(MuleMessage message, String endpointName) throws MuleException
    {
        session.dispatchEvent(message, endpointName);
    }

    /**
     * Depending on the session state this methods either Passes an event
     * asynchronously to the next available Mule component in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpoint The endpoint name to disptch the event through.
     * @throws org.mule.api.MuleException if the event fails to be processed by the
     *             service or the transport for the endpoint
     */
    public void dispatchEvent(MuleMessage message, OutboundEndpoint endpoint) throws MuleException
    {
        session.dispatchEvent(message, endpoint);
    }

    /**
     * Requests a synchronous receive of an event on the service
     * 
     * @param endpoint the endpoint identifing the endpointUri on ewhich the event
     *            will be received
     * @param timeout time in milliseconds before the request timesout
     * @return The requested event or null if the request times out
     * @throws org.mule.api.MuleException if the request operation fails
     */
    public MuleMessage requestEvent(InboundEndpoint endpoint, long timeout) throws MuleException
    {
        return session.requestEvent(endpoint, timeout);
    }

    /**
     * Requests a synchronous receive of an event on the service
     * 
     * @param endpointName the endpoint identifing the endpointUri on ewhich the
     *            event will be received
     * @param timeout time in milliseconds before the request timesout
     * @return The requested event or null if the request times out
     * @throws org.mule.api.MuleException if the request operation fails
     */
    public MuleMessage requestEvent(String endpointName, long timeout) throws MuleException
    {
        return session.requestEvent(endpointName, timeout);
    }

    /**
     * Requests a synchronous receive of an event on the service
     * 
     * @param endpointUri the endpointUri on which the event will be received
     * @param timeout time in milliseconds before the request timesout
     * @return The requested event or null if the request times out
     * @throws org.mule.api.MuleException if the request operation fails
     */
    public MuleMessage requestEvent(EndpointURI endpointUri, long timeout) throws MuleException
    {
        InboundEndpoint endpoint =
                getMuleContext().getRegistry().lookupEndpointFactory().getInboundEndpoint(endpointUri);
        return session.requestEvent(endpoint, timeout);
    }

    /**
     * @return the service descriptor of the service that received this event
     */
    public Service getService()
    {
        return event.getService();
    }

    /**
     * Determines whether the default processing for this event will be executed. By
     * default, the Mule server will route events according to a components
     * configuration. The user can override this behaviour by obtaining a reference
     * to the MuleEvent context, either by implementing
     * <code>org.mule.api.lifecycle.Callable</code> or calling
     * <code>RequestContext.getEventContext</code> to obtain the MuleEventContext for
     * the current thread. The user can programmatically control how events are
     * dispatched.
     * 
     * @return Returns true is the user has set stopFurtherProcessing.
     * @see org.mule.api.MuleEventContext
     * @see org.mule.api.lifecycle.Callable
     */
    public boolean isStopFurtherProcessing()
    {
        return RequestContext.getEvent().isStopFurtherProcessing();
    }

    /**
     * Determines whether the default processing for this event will be executed. By
     * default, the Mule server will route events according to a components
     * configuration. The user can override this behaviour by obtaining a reference
     * to the MuleEvent context, either by implementing
     * <code>org.mule.api.lifecycle.Callable</code> or calling
     * <code>RequestContext.getEventContext</code> to obtain the MuleEventContext for
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

    public EndpointURI getEndpointURI()
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
    public Transaction getTransaction()
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

    public MuleSession getSession()
    {
        return event.getSession();
    }

    public String toString()
    {
        return event.toString();
    }

    public MuleContext getMuleContext()
    {
        return event.getMuleContext();
    }

}
