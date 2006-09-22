/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.endpoint;

import org.mule.umo.UMOFilter;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.security.UMOEndpointSecurityFilter;
import org.mule.umo.transformer.UMOTransformer;

import java.io.Serializable;
import java.util.Map;

/**
 * <code>UMOImmutableEndpoint</code> describes a Message endpoint where data
 * is sent or received. An Enpoint is an Resource address (EndpointUri), with
 * associated transformation, transaction and filtering rules.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOImmutableEndpoint extends Serializable, Cloneable, Initialisable
{
    public static final String INITIAL_STATE_STARTED = "started";
    public static final String INITIAL_STATE_STOPPED = "stopped";

    /** The endpoint is outbound */
    public static final String ENDPOINT_TYPE_SENDER = "sender";

    /** The endpoint is indound */
     public static final String ENDPOINT_TYPE_RECEIVER = "receiver";

    /** The endpoint is either and will be set depending on how it is used */
     public static final String ENDPOINT_TYPE_SENDER_AND_RECEIVER = "senderAndReceiver";

    /** The endpoint is a receive endpoint set on a response router */
     public static final String ENDPOINT_TYPE_RESPONSE = "response";

    /**
     * This specifes the communication endpointUri. This will have a different
     * format depending on the transport protocol being used i.e.
     * <ul>
     * <li>smtp -&gt; admin&#64;mycompany.com</li>
     * <li>jms -&gt; shipping.orders.topic</li>
     * <li>sms -&gt; +447910010010</li>
     * </ul>
     * <p/> if an endpointUri is not specifed it will be assumed that it will be
     * determined at run-time by the calling application. The endpointUri can be
     * aliteral endpointUri such as an email address or it can be a logical name
     * for an endpointUri as long as it is declared in a <i>message-endpointUri</i>
     * block. When the message-provider is created the endpointUri is first
     * lookup in the endpointUri registry and if nothing is returned the
     * endpointUri value itself is used.
     * 
     * @return the endpointUri on which the endpoint sends or receives data
     */
    UMOEndpointURI getEndpointURI();

    /**
     * Decides the encoding to be used for events received by this endpoint
     * @return the encoding set on the endpoint or null if no codin has been specified
     */ 
    String getEncoding();

    /**
     * Determines whether the message endpoint is a sender or receiver or both.
     * The possible values are-
     * <ul>
     * <li>sender - PROVIDER_TYPE_SENDER</li>
     * <li>receiver - PROVIDER_TYPE_RECEIVER</li>
     * <li>senderAndReceiver - PROVIDER_TYPE_SENDER_AND_RECEIVER</li>
     * </ul>
     * The default is 'senderAndReceiver'.
     * 
     * @return the endpoint type
     */
    String getType();

    /**
     * The endpoint that will be used to send the message on. It is important
     * that the endpointUri and the connection correlate i.e. if your
     * endpointUri is a jms queue your connection must be a JMS endpoint.
     * 
     * @return the endpoint associated with the endpoint
     */
    UMOConnector getConnector();

    /**
     * The name is the identifier for the endpoint
     * 
     * @return the endpoint name
     */
    String getName();

    /**
     * The transformer is responsible for transforming data when it is received
     * or sent by the UMO (depending on whether this endpoint is a receiver or
     * not). A tranformation for an inbound event can be forced by the user by
     * calling the inbound event.getTransformedMessage(). A tranformation for an
     * outbound event is called or when the UMO dispatchEvent() or sendEvent()
     * methods are called. <p/> This attribute represents the name of the
     * transformer to use as declared in the transformers section of the
     * configuration file. IF a name for the transformer is not set on the
     * configuration element, it will default to the name of the className
     * attribute minus the package name.
     * 
     * @return the transformer to use when receiving or sending data
     */
    UMOTransformer getTransformer();

    /**
     * The transformer used when a response is returned from invoking this endpoint
     * @return the transformer to use when receiving the response data
     */
    UMOTransformer getResponseTransformer();

    /**
     * Returns any properties set on this endpoint
     * 
     * @return a map of properties for this endpoint
     */
    Map getProperties();

    /**
     * Retrieves a property set on the endpoint
     * @param key the name of the property
     * @return the property value or null if it does not exist
     */
    Object getProperty(Object key);

    /**
     * The transport protocol name that the message endpoint communicates over.
     * i.e. jms, sms, smtp etc. The protocol must match that of the associated
     * endpoint
     * 
     * @return the protocol name
     */
    String getProtocol();

    /**
     * @return true if this endpoint is read-only and none of it's properties
     *         can change. Global endpoints should be read-only so that
     *         unexpected behaviour is avoided.
     */
    boolean isReadOnly();

    /**
     * Determines whether this endpoint can be used to send events
     * 
     * @return true if it has been configured to send events, false otherwise
     */
    boolean canSend();

    /**
     * Determines whether this endpoint can be used to receive events
     * 
     * @return true if it has been configured to receive events, false otherwise
     */
    boolean canReceive();

    /**
     * Returns the transaction configuration for this endpoint
     * 
     * @return the transaction configuration for this endpoint or null if the
     *         endpoint is not transactional
     */
    UMOTransactionConfig getTransactionConfig();

    /**
     * Make a deep copy of this endpoint
     * 
     * @return a copy of the endpoint
     */
    Object clone();

    /**
     * The filter to apply to incoming messages. Only applies when the endpoint
     * endpointUri is a receiver
     * 
     * @return the UMOFilter to use or null if one is not set
     */
    UMOFilter getFilter();

    /**
     * If a filter is configured on this endpoint, this property will determine
     * if message that are not excepted by the filter are deleted
     * 
     * @return true if message should be deleted, false otherwise
     */
    boolean isDeleteUnacceptedMessages();

    /**
     * Returns an UMOEndpointSecurityFilter for this endpoint. If one is not
     * set, there will be no authentication on events sent via this endpoint
     * 
     * @return UMOEndpointSecurityFilter responsible for authenticating message
     *         flow via this endpoint.
     * @see UMOEndpointSecurityFilter
     */
    UMOEndpointSecurityFilter getSecurityFilter();

    /**
     * Determines if requests originating from this endpoint should be
     * synchronous i.e. execute in a single thread and possibly return an
     * result. This property is only used when the endpoint is of type
     * 'receiver'
     * 
     * @return whether requests on this endpoint should execute in a single
     *         thread. This property is only used when the endpoint is of type
     *         'receiver'
     */
    boolean isSynchronous();

    /**
     * Determines if the synchronous porperty has been set on the endpoint
     * @return
     */
    boolean isSynchronousSet();

    /**
     * For certain providers that support the notion of a backchannel such as sockets (outputStream) or
     * Jms (ReplyTo) Mule can automatically wait for a response from a backchannel when dispatching
     * over these protocols.  This is different for synchronous as synchronous behavior only applies to in
     * @return
     */
    boolean isRemoteSync();

    /**
     * The timeout value for remoteSync invocations
     * @return the timeout in milliseconds
     */
    int getRemoteSyncTimeout();

    /**
     * Determines if a new connector is created for this endpoint or an exising one must
     * already be present
     * @return
     */
    int getCreateConnector();

    /**
     * Sets the state the endpoint will be loaded in.  The States are
     * 'stopped' and 'started' (default)
     * @return the endpoint starting state
     */
    String getInitialState();

    /**
     * Determines whether the endpoint should deal with requests as streams
     * @return true if the request should be streamed
     */
    boolean isStreaming();
}
