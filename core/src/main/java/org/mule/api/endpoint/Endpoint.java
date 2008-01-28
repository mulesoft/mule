/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.endpoint;

import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transport.ConnectionStrategy;
import org.mule.api.transport.Connector;

import java.util.List;
import java.util.Map;

/**
 * <code>Endpoint</code> describes a Provider in the Mule Server. A endpoint is
 * a grouping of an endpoint, an endpointUri and a transformer.
 * 
 */
public interface Endpoint extends ImmutableEndpoint
{
    /**
     * This specifes the communication endpointUri. This will have a different format
     * depending on the transport protocol being used i.e.
     * <ul>
     * <li>smtp -&gt; smtp://admin&#64;mycompany.com</li>
     * <li>jms -&gt; jms://shipping.orders.topic</li>
     * <li>sms -&gt; sms://+447910010010</li>
     * </ul>
     * <p/> if an endpointUri is not specifed it will be assumed that it will be
     * determined at run-time by the calling application. The endpointUri can be
     * aliteral endpointUri such as an email address or it can be a logical name for
     * an endpointUri as long as it is declared in a <i>message-endpointUri</i>
     * block. When the message-provider is created the endpointUri is first lookup in
     * the endpointUri registry and if nothing is returned the endpointUri value
     * itself is used.
     * 
     * @param endpointUri the endpointUri on which the Endpoint sends or receives
     *            data
     * @throws EndpointException thrown if the EndpointUri cannot be processed by the
     *             Endpoint
     */
    void setEndpointURI(EndpointURI endpointUri) throws EndpointException;

    /**
     * Sets the encoding to be used for events received by this endpoint
     * 
     * @param endpointEncoding the encoding set on the endpoint. If not set the
     *            encoding will be taken from the manager config
     */
    void setEncoding(String endpointEncoding);

    /**
     * The endpoint that will be used to send the message on. It is important that
     * the endpointUri and the connection correlate i.e. if your endpointUri is a jms
     * queue your connection must be a JMS endpoint.
     * 
     * @param connector the endpoint to associate with the endpoint
     */
    void setConnector(Connector connector);

    /**
     * @param name the name to identify the endpoint
     */
    void setName(String name);

    /**
     * The transformers are responsible for transforming data when it is received or
     * sent by the UMO (depending on whether this endpoint is a receiver or not). A
     * tranformation for an inbound event can be forced by the user by calling the
     * inbound event.getTransformedMessage(). A tranformation for an outbound event
     * is called or when the UMO dispatchEvent() or sendEvent() methods are called.
     *
     * @param transformers the transformers to use when receiving or sending data
     */
    void setTransformers(List transformers);

    /**
     * Sets the transformers used when a response is sent back from the endpoint
     * invocation
     * 
     * @param transformers the transformers to use
     */
    void setResponseTransformers(List transformers);

    /**
     * @param props properties for this endpoint
     */
    void setProperties(Map props);

    /**
     * Returns the transaction configuration for this endpoint
     * 
     * @return transaction config for this endpoint
     */
    TransactionConfig getTransactionConfig();

    /**
     * Sets the Transaction configuration for the endpoint
     * 
     * @param config the transaction config to use by this endpoint
     */
    void setTransactionConfig(TransactionConfig config);

    /**
     * The filter to apply to incoming messages
     * 
     * @param filter the filter to use
     */
    void setFilter(Filter filter);

    /**
     * If a filter is configured on this endpoint, this property will determine if
     * message that are not excepted by the filter are deleted
     * 
     * @param delete if message should be deleted, false otherwise
     */
    void setDeleteUnacceptedMessages(boolean delete);

    /**
     * Sets an EndpointSecurityFilter for this endpoint. If a filter is set all
     * traffice via this endpoint with be subject to authentication.
     * 
     * @param filter the UMOSecurityFilter responsible for authenticating message
     *            flow via this endpoint.
     * @see org.mule.api.security.EndpointSecurityFilter
     */
    void setSecurityFilter(EndpointSecurityFilter filter);

    /**
     * Determines if requests originating from this endpoint should be synchronous
     * i.e. execute in a single thread and possibly return an result. This property
     * is only used when the endpoint is of type 'receiver'.
     * 
     * @param synchronous whether requests on this endpoint should execute in a
     *            single thread. This property is only used when the endpoint is of
     *            type 'receiver'
     */
    void setSynchronous(boolean synchronous);

    /**
     * Sets a property on the endpoint
     * 
     * @param key the property key
     * @param value the value of the property
     */
    void setProperty(String key, Object value);

    /**
     * For certain providers that support the notion of a backchannel such as sockets
     * (outputStream) or Jms (ReplyTo) Mule can automatically wait for a response
     * from a backchannel when dispatching over these protocols. This is different
     * for synchronous as synchronous behavior only applies to in
     * 
     * @param value whether the endpoint should perfrom sync receives
     */
    void setRemoteSync(boolean value);

    /**
     * The timeout value for remoteSync invocations
     * 
     * @param timeout the timeout in milliseconds
     */
    void setRemoteSyncTimeout(int timeout);

    /**
     * Sets the state the endpoint will be loaded in. The States are 'stopped' and
     * 'started' (default)
     * 
     * @param state
     */
    void setInitialState(String state);
    
    /**
     * Sets the connection strategy this endpoint should use when connecting to the underlying resource
     * @param connectionStrategy the connection strategy this endpoint should use when connecting to the underlying resource
     */
     void setConnectionStrategy(ConnectionStrategy connectionStrategy);
}
