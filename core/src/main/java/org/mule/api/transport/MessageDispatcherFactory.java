/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.transport;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;

import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * <code>MessageDispatcherFactory</code> is a factory interface for managing the
 * lifecycles of a transport's message dispatchers. The methods basically implement
 * the {@link KeyedPoolableObjectFactory} lifecycle, with a
 * {@link ImmutableEndpoint} as the key and the dispatcher as pooled object.
 */
public interface MessageDispatcherFactory
{

    /**
     * Controls whether dispatchers are cached or created per request. Note that if
     * an exception occurs in the dispatcher, it is automatically disposed of and a
     * new one is created for the next request. This allows dispatchers to recover
     * from loss of connection and other faults. When invoked by
     * {@link #validate(OutboundEndpoint, MessageDispatcher)} it takes
     * precedence over the dispatcher's own return value of
     * {@link MessageDispatcher#validate()}.
     *
     * @return true if created per request
     */
    boolean isCreateDispatcherPerRequest();

    /**
     * Creates a new message dispatcher instance, initialised with the passed
     * endpoint. The returned instance should be immediately useable.
     * 
     * @param endpoint the endoint for which this dispatcher should be created
     * @return a properly created <code>MessageDispatcher</code> for this
     *         transport
     * @throws MuleException if the dispatcher cannot be created
     */
    MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException;

    /**
     * Invoked <strong>before</strong> the given dispatcher is handed out to a
     * client, but <strong>not</strong> after {@link #create(OutboundEndpoint)}.
     * 
     * @param endpoint the endpoint of the dispatcher
     * @param dispatcher the dispatcher to be activated
     * @throws MuleException if the dispatcher cannot be activated
     */
    void activate(OutboundEndpoint endpoint, MessageDispatcher dispatcher) throws MuleException;

    /**
     * Invoked <strong>after</strong> the dispatcher is returned from a client but
     * <strong>before</strong> it is prepared for return to its pool via
     * {@link #passivate(OutboundEndpoint, MessageDispatcher)}.
     * 
     * @param endpoint the endpoint of the dispatcher
     * @param dispatcher the dispatcher to be validated
     * @return <code>true</code> if the dispatcher is valid for reuse,
     *         <code>false</code> otherwise.
     */
    boolean validate(OutboundEndpoint endpoint, MessageDispatcher dispatcher);

    /**
     * Invoked immediately <strong>before</strong> the given dispatcher is returned
     * to its pool.
     * 
     * @param endpoint the endpoint of the dispatcher
     * @param dispatcher the dispatcher to be passivated
     */
    void passivate(OutboundEndpoint endpoint, MessageDispatcher dispatcher);

    /**
     * Invoked when a dispatcher returned <code>false</code> for
     * {@link #validate(OutboundEndpoint, MessageDispatcher)}.
     * 
     * @param endpoint the endpoint of the dispatcher
     * @param dispatcher the dispatcher to be validated
     */
    void destroy(OutboundEndpoint endpoint, MessageDispatcher dispatcher);

}
