/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.endpoint;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.NamedObject;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transport.Connector;
import org.mule.processor.AbstractRedeliveryPolicy;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * <code>ImmutableEndpoint</code> describes a Message endpoint where data is
 * sent or received. An Enpoint is an Resource address (EndpointUri), with associated
 * transformation, transaction and filtering rules.
 */
public interface ImmutableEndpoint extends Serializable, NamedObject
{

    String INITIAL_STATE_STARTED = "started";

    /**
     * This specifess the communication endpointUri. This will have a different format
     * depending on the transport protocol being used i.e.
     * <ul>
     * <li>smtp -&gt; admin&#64;mycompany.com</li>
     * <li>jms -&gt; shipping.orders.topic</li>
     * <li>sms -&gt; +447910010010</li>
     * </ul>
     * <p/> if an endpointUri is not specifed it will be assumed that it will be
     * determined at run-time by the calling application. The endpointUri can be
     * aliteral endpointUri such as an email address or it can be a logical name for
     * an endpointUri as long as it is declared in a <i>message-endpointUri</i>
     * block. When the message-provider is created the endpointUri is first lookup in
     * the endpointUri registry and if nothing is returned the endpointUri value
     * itself is used.
     *
     * @return the endpointUri on which the endpoint sends or receives data
     */
    EndpointURI getEndpointURI();

    /**
     * This returns the address of the endpoint.  When this contains a template, it may not be a URI
     *
     * @return the address on which the endpoint sends or receives data
     */
    String getAddress();

    /**
     * Decides the encoding to be used for events received by this endpoint
     *
     * @return the encoding set on the endpoint or null if no codin has been
     *         specified
     */
    String getEncoding();

    /**
     * The endpoint that will be used to send the message on. It is important that
     * the endpointUri and the connection correlate i.e. if your endpointUri is a jms
     * queue your connection must be a JMS endpoint.
     *
     * @return the endpoint associated with the endpoint
     */
    Connector getConnector();

    /**
     * Returns any properties set on this endpoint
     *
     * @return a map of properties for this endpoint
     */
    Map getProperties();

    /**
     * Retrieves a property set on the endpoint
     *
     * @param key the name of the property
     * @return the property value or null if it does not exist
     */
    Object getProperty(Object key);

    /**
     * The transport protocol name that the message endpoint communicates over. i.e.
     * jms, sms, smtp etc. The protocol must match that of the associated endpoint
     *
     * @return the protocol name
     */
    String getProtocol();

    /**
     * @return true if this endpoint is read-only and none of it's properties can
     *         change. Global endpoints should be read-only so that unexpected
     *         behaviour is avoided.
     */
    boolean isReadOnly();

    /**
     * Returns the transaction configuration for this endpoint
     *
     * @return the transaction configuration for this endpoint or null if the
     *         endpoint is not transactional
     */
    TransactionConfig getTransactionConfig();

    /**
     * The filter to apply to incoming messages. Only applies when the endpoint
     * endpointUri is a receiver
     *
     * @return the Filter to use or null if one is not set
     */
    Filter getFilter();

    /**
     * If a filter is configured on this endpoint, this property will determine if
     * message that are not excepted by the filter are deleted
     *
     * @return true if message should be deleted, false otherwise
     */
    boolean isDeleteUnacceptedMessages();

    /**
     * Returns an EndpointSecurityFilter for this endpoint. If one is not set,
     * there will be no authentication on events sent via this endpoint
     *
     * @return EndpointSecurityFilter responsible for authenticating message flow
     *         via this endpoint.
     * @see EndpointSecurityFilter
     */
    @Deprecated
    EndpointSecurityFilter getSecurityFilter();

    EndpointMessageProcessorChainFactory getMessageProcessorsFactory();

    List <MessageProcessor> getMessageProcessors();

    List <MessageProcessor> getResponseMessageProcessors();

    MessageExchangePattern getExchangePattern();

    /**
     * The timeout value for waiting for a response from a remote invocation or back channel. Mule
     * will only wait for a response if the endpoint's message exchange pattern requires a
     * response.
     *
     * @return the timeout in milliseconds
     */
    int getResponseTimeout();

    /**
     * Sets the state the endpoint will be loaded in. The States are 'stopped' and
     * 'started' (default)
     *
     * @return the endpoint starting state
     */
    String getInitialState();

    MuleContext getMuleContext();

    /**
     * The retry policy on the endpoint configures how retries are handled. The behaviour is slightly different
     * for inbound and outbound endpoints.
     * For inbound endpoints the Retry Policy determines how the connection to the underlying transport will be
     * managed if the connection is lost.
     * For outbound endpoints, the Retry Policy will attempt to retry dispatching, sending and receiving an event
     *
     * @return the Policy factory to use when retrying a connection or dispatching an event
     */
    RetryPolicyTemplate getRetryPolicyTemplate();

    /**
     * The name of the endpoint builder used to create this endpoint. May be used to
     * an endpoints builder for example to recreate endpoints for deserialized events.
     */
    String getEndpointBuilderName();

    boolean isProtocolSupported(String protocol);

    /**
     * Return the mime type defined for the endpoint, if any
     */
    String getMimeType();

    /**
     * Return the endpoint's redelivery policy, if any
     */
    AbstractRedeliveryPolicy getRedeliveryPolicy();

    boolean isDisableTransportTransformer();

}
