/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.transport;

import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;

/**
 * A factory interface for managing the lifecycles of a transport's message requesters.
 * The methods basically implement
 * the {@link org.apache.commons.pool.KeyedPoolableObjectFactory} lifecycle, with a
 * {@link org.mule.api.endpoint.InboundEndpoint} as the key and the requester as pooled object.
 */
public interface MessageRequesterFactory
{

    /**
     * Controls whether dispatchers are cached or created per request. Note that if
     * an exception occurs in the requester, it is automatically disposed of and a
     * new one is created for the next request. This allows requesters to recover
     * from loss of connection and other faults. When invoked by
     * {@link #validate(org.mule.api.endpoint.InboundEndpoint, org.mule.api.transport.MessageRequester)} it takes
     * precedence over the dispatcher's own return value of
     * {@link org.mule.api.transport.MessageDispatcher#validate()}.
     *
     * @return true if created per request
     */
    boolean isCreateRequesterPerRequest();

    /**
     * Creates a new message requester instance, initialised with the passed
     * endpoint. The returned instance should be immediately useable.
     *
     * @param endpoint the endoint for which this requester should be created
     * @return a properly created <code>MessageRequester</code> for this
     *         transport
     * @throws org.mule.api.MuleException if the requester cannot be created
     */
    MessageRequester create(InboundEndpoint endpoint) throws MuleException;

    /**
     * Invoked <strong>before</strong> the given requester is handed out to a
     * client, but <strong>not</strong> after {@link #create(org.mule.api.endpoint.InboundEndpoint)}.
     *
     * @param endpoint the endpoint of the requester
     * @param requester the requester to be activated
     * @throws org.mule.api.MuleException if the requester cannot be activated
     */
    void activate(InboundEndpoint endpoint, MessageRequester requester) throws MuleException;

    /**
     * Invoked <strong>after</strong> the requester is returned from a client but
     * <strong>before</strong> it is prepared for return to its pool via
     * {@link #passivate(org.mule.api.endpoint.InboundEndpoint, org.mule.api.transport.MessageRequester)}.
     *
     * @param endpoint the endpoint of the requester
     * @param requester the requester to be validated
     * @return <code>true</code> if the requester is valid for reuse,
     *         <code>false</code> otherwise.
     */
    boolean validate(InboundEndpoint endpoint, MessageRequester requester);

    /**
     * Invoked immediately <strong>before</strong> the given requester is returned
     * to its pool.
     *
     * @param endpoint the endpoint of the requester
     * @param requester the requester to be passivated
     */
    void passivate(InboundEndpoint endpoint, MessageRequester requester);

    /**
     * Invoked when a requester returned <code>false</code> for
     * {@link #validate(org.mule.api.endpoint.InboundEndpoint, org.mule.api.transport.MessageRequester)}.
     *
     * @param endpoint the endpoint of the requester
     * @param requester the requester to be validated
     */
    void destroy(InboundEndpoint endpoint, MessageRequester requester);

}
