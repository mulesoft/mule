/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.transport;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.NameableObject;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleStateEnabled;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.model.streaming.CallbackOutputStream;
import org.mule.processor.AbstractRedeliveryPolicy;

import java.io.OutputStream;
import java.util.List;

/**
 * <code>Connector</code> is the mechanism used to connect to external systems
 * and protocols in order to send and receive data.
 */
public interface Connector extends Lifecycle, NameableObject, Connectable, LifecycleStateEnabled
{
    int INT_VALUE_NOT_SET = -1;

    /**
     * Registers a MessageProcessor listener which will listen to new message
     * received from a specific transport channel and then processed by the endpoint.
     * Only a single listener can be registered for a given endpoints. Attempts to
     * register a listener when one is already registered will fail.
     *
     * @param endpoint defines both the transport and channel/resource uri as well
     *            the processing (transformation/filtering) that should occur when
     *            the endpoint processes a new message from the transport receiver.
     * @param listener the listener that will be invoked when messages are received
     *            on the endpoint.
     * @param flowConstruct reference to the flow construct that the listener is part
     *            of for use as context for logging, notifications and error
     *            handling.
     */
    public void registerListener(InboundEndpoint endpoint, MessageProcessor listener, FlowConstruct flowConstruct)
        throws Exception;

    /**
     * Unregisters the listener for the given endpoints. This will mean that the
     * listener that was registered for this endpoint will no longer receive any
     * messages.
     */
    public void unregisterListener(InboundEndpoint endpoint, FlowConstruct flowConstruct) throws Exception;

    /**
     * @return true if the endpoint is started
     */
    boolean isStarted();

    boolean isConnected();

    /**
     * @return false if the connector is alive and well or true if the connector is
     *         being destroyed
     */
    boolean isDisposed();

    /**
     * Creates a new {@link MuleMessageFactory} using what's defined in the connector's
     * transport service descriptor.
     */
    MuleMessageFactory createMuleMessageFactory() throws CreateException;

    /**
     * @return the primary protocol name for endpoints of this connector
     */
    String getProtocol();

    /**
     * @return true if the protocol is supported by this connector.
     */
    boolean supportsProtocol(String protocol);

    /**
     * The dispatcher factory is used to create a message dispatcher of the current
     * request
     *
     * @param factory the factory to use when a dispatcher request is madr
     */
    void setDispatcherFactory(MessageDispatcherFactory factory);

    /**
     * The dispatcher factory is used to create a message dispatcher of the current
     * request
     *
     * @return the factory to use when a dispatcher request is madr
     */
    MessageDispatcherFactory getDispatcherFactory();

    /**
     * The requester factory is used to create a message requester of the current
     * request
     *
     * @param factory the factory to use when a request is made
     */
    void setRequesterFactory(MessageRequesterFactory factory);

    /**
     * The requester factory is used to create a message requester of the current
     * request
     *
     * @return the factory to use when a request is made
     */
    MessageRequesterFactory getRequesterFactory();

    boolean isResponseEnabled();

    /**
     * Make a specific request to the underlying transport
     *
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a MuleMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    MuleMessage request(InboundEndpoint endpoint, long timeout) throws Exception;

    /**
     * Will get the output stream for this type of transport. Typically this will be
     * called only when Streaming is being used on an outbound endpoint. If Streaming
     * is not supported by this transport an {@link UnsupportedOperationException} is
     * thrown. Note that the stream MUST release resources on close. For help doing
     * so, see {@link CallbackOutputStream}.
     *
     * @param endpoint the endpoint that releates to this Dispatcher
     * @param event the current event being processed
     * @return the output stream to use for this request
     */
    OutputStream getOutputStream(OutboundEndpoint endpoint, MuleEvent event) throws MuleException;

    /**
     * Only use this method to use the Connector's MuleContext. Otherwise you can be used
     * the wrong MuleContext because a Connector can be defined at the domain level or de app level.
     *
     * @return MuleContext in which this connector has been created.
     *         If the Connector was defined in a Domain then it will return the MuleContext of the domain.
     *         If the Connector was defined in a Mule app then it will return the MuleContext of the Mule app.
     */
    MuleContext getMuleContext();

    RetryPolicyTemplate getRetryPolicyTemplate();

    /**
     * @return the default {@link MessageExchangePattern} as configured in the
     *         transport's service descriptor.
     */
    MessageExchangePattern getDefaultExchangePattern();

    /**
     * @return List of exchange patterns that this connector supports for inbound endpoints.
     */
    List<MessageExchangePattern> getInboundExchangePatterns();

    /**
     * @return List of exchange patterns that this connector supports for outbound endpoints.
     */
    List<MessageExchangePattern> getOutboundExchangePatterns();

    /**
     * @return The strategy used for reading and writing session information to and from the transport.
     */
    SessionHandler getSessionHandler();

    /**
     * @param maxRedelivery times to try message redelivery
     * @return AbstractRedeliveryPolicy to use for message redelivery, null if it shouldn't be used
     */
    AbstractRedeliveryPolicy createDefaultRedeliveryPolicy(int maxRedelivery);

    /**
     * Returns a canonical representation of the given {@link org.mule.api.endpoint.EndpointURI}
     *
     * @param uri a not null {@link org.mule.api.endpoint.EndpointURI}
     * @return the canonical representation of the given uri as a {@link java.lang.String}
     */
    public String getCanonicalURI(EndpointURI uri);
}
