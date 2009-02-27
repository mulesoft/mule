/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api;

import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transformer.TransformerException;

import java.io.OutputStream;

/**
 * <code>MuleEventContext</code> is the context object for the current request.
 * Using the context, developers can send/dispatch/receive events programmatically as
 * well as manage transactions.
 */
public interface MuleEventContext
{
    /**
     * Returns the message payload for this event
     * 
     * @return the message payload for this event
     */
    MuleMessage getMessage();

    /**
     * Returns the contents of the message as a byte array.
     * 
     * @return the contents of the message as a byte array
     * @throws MuleException if the message cannot be converted into an array of bytes
     */
    byte[] getMessageAsBytes() throws MuleException;

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
    Object transformMessage() throws TransformerException;

    /**
     * Returns the message transformed into it's recognised or expected format. The
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
    Object transformMessage(Class expectedType) throws TransformerException;

    /**
     * Returns the message transformed into it's recognised or expected format and
     * then into an array of bytes. The transformer used is the one configured on the
     * endpoint through which this event was received.
     * 
     * @return the message transformed into it's recognised or expected format as an
     *         array of bytes.
     * @throws TransformerException if a failure occurs in the transformer
     * @see org.mule.api.transformer.Transformer
     */
    byte[] transformMessageToBytes() throws TransformerException;

    /**
     * Returns the message transformed into it's recognised or expected format and
     * then into a String. The transformer used is the one configured on the endpoint
     * through which this event was received. This method will use the encoding set
     * on the event
     * 
     * @return the message transformed into it's recognised or expected format as a
     *         Strings.
     * @throws TransformerException if a failure occurs in the transformer
     * @see org.mule.api.transformer.Transformer
     */
    String transformMessageToString() throws TransformerException;

    /**
     * Returns the message contents as a string This method will use the encoding set
     * on the event
     * 
     * @return the message contents as a string
     * @throws MuleException if the message cannot be converted into a string
     */
    String getMessageAsString() throws MuleException;

    /**
     * Returns the message contents as a string
     * 
     * @param encoding The encoding to use when transforming the message
     * @return the message contents as a string
     * @throws MuleException if the message cannot be converted into a string
     */
    String getMessageAsString(String encoding) throws MuleException;

    /**
     * Returns the current transaction (if any) for the session
     * 
     * @return the current transaction for the session or null if there is no
     *         transaction in progress
     */
    Transaction getCurrentTransaction();

    /**
     * Mark the current transaction (if any) for rollback
     * 
     * @throws TransactionException
     */
    void markTransactionForRollback() throws TransactionException;

    /**
     * This will send an event via the configured outbound router on the service
     * 
     * @param message the message to send
     * @return the result of the send if any
     * @throws MuleException if there is no outbound endpoint configured on the
     *             service or the events fails during dispatch
     */
    MuleMessage sendEvent(Object message) throws MuleException;

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule component in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the message payload to send
     * @return the return Message from the call or null if there was no result
     * @throws MuleException if the event fails to be processed by the service or
     *             the transport for the endpoint
     */
    MuleMessage sendEvent(MuleMessage message) throws MuleException;

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule component in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpoint The endpointUri to disptch the event through
     * @return the return Message from the call or null if there was no result
     * @throws MuleException if the event fails to be processed by the service or
     *             the transport for the endpoint
     */
    MuleMessage sendEvent(MuleMessage message, EndpointURI endpoint) throws MuleException;

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
     * @throws MuleException if the event fails to be processed by the service or
     *             the transport for the endpoint
     */
    MuleMessage sendEvent(MuleMessage message, String endpointName) throws MuleException;

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule component in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpoint The endpoint to disptch the event through.
     * @return the return Message from the call or null if there was no result
     * @throws MuleException if the event fails to be processed by the service or
     *             the transport for the endpoint
     */
    MuleMessage sendEvent(MuleMessage message, OutboundEndpoint endpoint) throws MuleException;

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
     * @see FutureMessageResult
     */
    FutureMessageResult sendEventAsync(Object message, int timeout) throws MuleException;

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
     * @see FutureMessageResult
     */
    FutureMessageResult sendEventAsync(MuleMessage message, int timeout) throws MuleException;

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
     * @param endpoint the endpointUri to dispatch to
     * @param timeout how long to block in milliseconds waiting for a result
     * @return the result message if any of the invocation
     * @throws org.mule.api.MuleException if the dispatch fails or the components or
     *             transfromers cannot be found
     * @see FutureMessageResult
     */
    FutureMessageResult sendEventAsync(MuleMessage message, EndpointURI endpoint, int timeout)
        throws MuleException;

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
     * @see FutureMessageResult
     */
    FutureMessageResult sendEventAsync(MuleMessage message, String endpointName, int timeout)
        throws MuleException;

    /**
     * This will dispatch an event asynchronously via the configured outbound
     * endpoint on the service for this session
     * 
     * @param message the message to send
     * @throws MuleException if there is no outbound endpoint configured on the
     *             service or the events fails during dispatch
     */
    void dispatchEvent(MuleMessage message) throws MuleException;

    /**
     * This will dispatch an event asynchronously via the configured outbound
     * endpoint on the service for this session
     * 
     * @param payload the message payloadto send
     * @throws MuleException if there is no outbound endpoint configured on the
     *             service or the events fails during dispatch
     */
    void dispatchEvent(Object payload) throws MuleException;

    /**
     * Depending on the session state this methods either Passes an event
     * asynchronously to the next available Mule component in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpoint the endpointUri to dispatc the event to first on the service
     *            configuration and then on the mule manager configuration
     * @throws MuleException if the event fails to be processed by the service or
     *             the transport for the endpoint
     */
    void dispatchEvent(MuleMessage message, EndpointURI endpoint) throws MuleException;

    /**
     * Depending on the session state this methods either Passes an event
     * asynchronously to the next available Mule component in the pool or via the endpoint
     * configured for the event.
     * 
     * @param message the event message payload to send
     * @param endpointName The endpoint name to disptch the event through. This will
     *            be looked up first on the service configuration and then on the
     *            mule manager configuration
     * @throws MuleException if the event fails to be processed by the service or
     *             the transport for the endpoint
     */
    void dispatchEvent(MuleMessage message, String endpointName) throws MuleException;

    /**
     * Depending on the session state this methods either Passes an event
     * asynchronously to the next available Mule component in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpoint The endpoint name to disptch the event through.
     * @throws MuleException if the event fails to be processed by the service or
     *             the transport for the endpoint
     */
    void dispatchEvent(MuleMessage message, OutboundEndpoint endpoint) throws MuleException;

    /**
     * Requests a synchronous receive of an event on the service.
     * 
     * @param endpoint the endpoint identifying the endpointUri on which the event
     *            will be received
     * @param timeout time in milliseconds before the request times out
     * @return The requested event or null if the request times out
     * @throws MuleException if the request operation fails
     */
    MuleMessage requestEvent(InboundEndpoint endpoint, long timeout) throws MuleException;

    /**
     * Requests a synchronous receive of an event on the service.
     * 
     * @param endpointName the endpoint identifying the endpointUri on which the
     *            event will be received
     * @param timeout time in milliseconds before the request timesout
     * @return The requested event or null if the request times out
     * @throws MuleException if the request operation fails
     */
    MuleMessage requestEvent(String endpointName, long timeout) throws MuleException;

    /**
     * Requests a synchronous receive of an event on the service.
     * 
     * @param endpoint the endpointUri on which the event will be received
     * @param timeout time in milliseconds before the request timesout
     * @return The requested event or null if the request times out
     * @throws MuleException if the request operation fails
     */
    MuleMessage requestEvent(EndpointURI endpoint, long timeout) throws MuleException;

    Service getService();

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
     * @return Returns true is the user has set stopFurtherProcessing.
     * @see org.mule.api.MuleContext
     * @see MuleEventContext
     * @see org.mule.api.lifecycle.Callable
     */
    boolean isStopFurtherProcessing();

    /**
     * Determines whether the default processing for this event will be executed. By
     * default, the Mule server will route events according to a components
     * configuration. The user can override this behaviour by obtaining a reference
     * to the MuleEvent context, either by implementing
     * <code>org.mule.api.lifecycle.Callable</code> or calling
     * <code>UMOManager.getEventContext</code> to obtain the MuleEventContext for
     * the current thread. The user can programmatically control how events are
     * dispached.
     * 
     * @param stopFurtherProcessing the value to set.
     */
    void setStopFurtherProcessing(boolean stopFurtherProcessing);

    /**
     * An outputstream the can optionally be used write response data to an incoming
     * message.
     * 
     * @return an output strem if one has been made available by the message receiver
     *         that received the message
     */
    OutputStream getOutputStream();

    /**
     * Determines whether the was sent synchrounously or not
     * 
     * @return true if the event is synchronous
     */
    boolean isSynchronous();

    /**
     * Returns a reference to the Endpoint Uri for this context This is the endpoint
     * on which the event was received
     * 
     * @return the receive endpoint for this event context
     */
    EndpointURI getEndpointURI();

    /**
     * Returns the transaction for the current event or null if there is no
     * transaction in progresss
     * 
     * @return the transaction for the current event or null if there is no
     *         transaction in progresss
     */
    Transaction getTransaction();

    /**
     * Get the timeout value associated with the event
     * 
     * @return the timeout for the event
     */
    int getTimeout();

    /**
     * Gets the encoding for the current message. For potocols that send encoding
     * Information with the message, this method should be overriden to expose the
     * transport encoding, otherwise the default encoding in the Mule configuration
     * will be used
     * 
     * @return the encoding for this message. This method must never return null
     */
    String getEncoding();

    MuleSession getSession();

    MuleContext getMuleContext();
}
