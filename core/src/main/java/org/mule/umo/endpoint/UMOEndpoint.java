/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.endpoint;

import org.mule.providers.ConnectionStrategy;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.security.UMOEndpointSecurityFilter;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Map;

/**
 * <code>UMOEndpoint</code> describes a Provider in the Mule Server. A endpoint is
 * a grouping of an endpoint, an endpointUri and a transformer.
 * 
 */
public interface UMOEndpoint extends UMOImmutableEndpoint
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
    void setEndpointURI(UMOEndpointURI endpointUri) throws EndpointException;

    /**
     * Sets the encoding to be used for events received by this endpoint
     * 
     * @param endpointEncoding the encoding set on the endpoint. If not set the
     *            encoding will be taken from the manager config
     */
    void setEncoding(String endpointEncoding);

    /**
     * Determines whether the message endpoint is a sender or receiver or both. The
     * possible values are-
     * <ul>
     * <li>sender - PROVIDER_TYPE_SENDER</li>
     * <li>receiver - PROVIDER_TYPE_RECEIVER</li>
     * <li>senderAndReceiver - PROVIDER_TYPE_SENDER_AND_RECEIVER</li>
     * </ul>
     * 
     * @param type the endpoint type
     */
    void setType(String type);

    /**
     * The endpoint that will be used to send the message on. It is important that
     * the endpointUri and the connection correlate i.e. if your endpointUri is a jms
     * queue your connection must be a JMS endpoint.
     * 
     * @param connector the endpoint to associate with the endpoint
     */
    void setConnector(UMOConnector connector);

    /**
     * @param name the name to identify the endpoint
     */
    void setName(String name);

    /**
     * The transformer is responsible for transforming data when it is received or
     * sent by the UMO (depending on whether this endpoint is a receiver or not). A
     * tranformation for an inbound event can be forced by the user by calling the
     * inbound event.getTransformedMessage(). A tranformation for an outbound event
     * is called or when the UMO dispatchEvent() or sendEvent() methods are called.
     * <p/> This attribute represents the name of the transformer to use as declared
     * in the transformers section of the configuration file. IF a name for the
     * transformer is not set on the configuration element, it will default to the
     * name of the className attribute minus the package name.
     * 
     * @param trans the transformer to use when receiving or sending data
     */
    void setTransformer(UMOTransformer trans);

    /**
     * Sets tyhe transformer used when a response is sent back from the endpoint
     * invocation
     * 
     * @param trans the transformer to use
     */
    void setResponseTransformer(UMOTransformer trans);

    /**
     * @param props properties for this endpoint
     */
    void setProperties(Map props);

    /**
     * Returns the transaction configuration for this endpoint
     * 
     * @return transaction config for this endpoint
     */
    UMOTransactionConfig getTransactionConfig();

    /**
     * Sets the Transaction configuration for the endpoint
     * 
     * @param config the transaction config to use by this endpoint
     */
    void setTransactionConfig(UMOTransactionConfig config);

    /**
     * The filter to apply to incoming messages
     * 
     * @param filter the filter to use
     */
    void setFilter(UMOFilter filter);

    /**
     * If a filter is configured on this endpoint, this property will determine if
     * message that are not excepted by the filter are deleted
     * 
     * @param delete if message should be deleted, false otherwise
     */
    void setDeleteUnacceptedMessages(boolean delete);

    /**
     * Sets an UMOEndpointSecurityFilter for this endpoint. If a filter is set all
     * traffice via this endpoint with be subject to authentication.
     * 
     * @param filter the UMOSecurityFilter responsible for authenticating message
     *            flow via this endpoint.
     * @see org.mule.umo.security.UMOEndpointSecurityFilter
     */
    void setSecurityFilter(UMOEndpointSecurityFilter filter);

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
     * This attribute determines how a connector is obtained for the endpoint. The
     * options are - GET_OR_CREATE_CONNECTOR = 0: create a connector for the endpoint
     * if one isn't already register (default) ALWAYS_CREATE_CONNECTOR = 1: Always
     * create a new connector of each endpoint NEVER_CREATE_CONNECTOR = 2: Throw an
     * exception if there is not connector with a matching protocol for this endpoint
     * 
     * @param action
     */
    void setCreateConnector(int action);

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
     * Determines whether the endpoint should deal with requests as streams
     * 
     * @param stream true if the request should be streamed
     */
    void setStreaming(boolean stream);

    /**
     * Sets the connection strategy this endpoint should use when connecting to the underlying resource
     * @param connectionStrategy the connection strategy this endpoint should use when connecting to the underlying resource
     */
     void setConnectionStrategy(ConnectionStrategy connectionStrategy);
}
