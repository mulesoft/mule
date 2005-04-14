/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.umo.endpoint;

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
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOEndpoint extends UMOImmutableEndpoint
{
    /**
     * This specifes the communication endpointUri.  This will have a different format depending
     * on the transport protocol being used i.e.
     * <ul>
     * <li>smtp -&gt; smtp://admin&#64;mycompany.com</li>
     * <li>jms  -&gt; jms://shipping.orders.topic</li>
     * <li>sms  -&gt; sms://+447910010010</li>
     * </ul>
     * <p/>
     * if an endpointUri is not specifed it will be assumed that it will be determined at run-time
     * by the calling application.  The endpointUri can be aliteral endpointUri such as an email
     * address or it can be a logical name for an endpointUri as long as it is declared in a
     * <i>message-endpointUri</i> block.  When the message-provider is created the endpointUri is first
     * lookup in the endpointUri registry and if nothing is returned the endpointUri value itself
     * is used.
     *
     * @param endpointUri the endpointUri on which the Endpoint sends or
     * receives data
     * @throws EndpointException thrown if the EndpointUri cannot be processed by the
     * Endpoint
     */
    public void setEndpointURI(UMOEndpointURI endpointUri) throws EndpointException;

    /**
     * Determines whether the message endpoint is a sender or receiver or both. The possible values are-
     * <ul>
     * <li>sender - PROVIDER_TYPE_SENDER</li>
     * <li>receiver - PROVIDER_TYPE_RECEIVER</li>
     * <li>senderAndReceiver - PROVIDER_TYPE_SENDER_AND_RECEIVER</li>
     * </ul>
     *
     * @param type the endpoint type
     */
    public void setType(String type);

    /**
     * The endpoint that will be used to send the message on. It is important that the endpointUri
     * and the connection correlate i.e. if your endpointUri is a jms queue your connection must
     * be a JMS endpoint.
     *
     * @param connector the endpoint to associate with the endpoint
     */
    public void setConnector(UMOConnector connector);

    /**
     * @param name the name to identify the endpoint
     */
    public void setName(String name);

    /**
     * The transformer is responsible for transforming data when it is received or sent by the UMO
     * (depending on whether this endpoint is a receiver or not). A tranformation for an
     * inbound event can be forced by the user by calling the inbound event.getTransformedMessage().
     * A tranformation for an outbound event is called or when the UMO dispatchEvent()
     * or sendEvent() methods are called.
     * <p/>
     * This attribute represents the name of the transformer to use as declared in the transformers
     * section of the configuration file. IF a name for the transformer is not set on the configuration
     * element, it will default to the name of the className attribute minus the package name.
     *
     * @param trans the transformer to use when receiving or sending data
     */
    public void setTransformer(UMOTransformer trans);

    /**
     * @param props properties for this endpoint
     */
    public void setProperties(Map props);

    /**
     * Returns the transaction configuration for this endpoint
     *
     * @return transaction config for this endpoint
     */
    public UMOTransactionConfig getTransactionConfig();

    /**
     * Sets the Transaction configuration for the endpoint
     *
     * @param config the transaction config to use by this endpoint
     */
    public void setTransactionConfig(UMOTransactionConfig config);

    /**
     * The filter to apply to incoming messages
     * @param filter the filter to use
     */
    public void setFilter(UMOFilter filter);

    /**
     * If a filter is configured on this endpoint, this property will
     * determine if message that are not excepted by the filter are deleted
     * @param delete if message should be deleted, false otherwise
     */
    public void setDeleteUnacceptedMessages(boolean delete);

    /**
     * Sets an UMOEndpointSecurityFilter for this endpoint.  If a filter is
     * set all traffice via this endpoint with be subject to authentication.
     * @param filter the UMOSecurityFilter responsible for authenticating
     * message flow via this endpoint.
     * @see org.mule.umo.security.UMOEndpointSecurityFilter
     */
    public void setSecurityFilter(UMOEndpointSecurityFilter filter);

    /**
     * Determines if requests originating from this endpoint should be synchronous
     * i.e. execute in a single thread and possibly return an result.  This property
     * is only used when the endpoint is of type 'receiver'.
     * @param synchronous whether requests on this endpoint should execute in a single
     * thread. This property is only used when the endpoint is of type 'receiver'
     */
    public void setSynchronous(boolean synchronous);

    void setCreateConnector(int action);
}