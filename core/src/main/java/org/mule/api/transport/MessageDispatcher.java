/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.transport;

import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.Disposable;

/**
 * Combine {@link MessageDispatching} with
 * various lifecycle methods for the actual instances doing message sending.
 */
public interface MessageDispatcher extends Connectable, MessageDispatching
{

    /**
     * This method can perform necessary state updates before any of the
     * {@link MessageDispatching} methods are invoked.
     * 
     * @see MessageDispatcherFactory#activate(org.mule.api.endpoint.ImmutableEndpoint, MessageDispatcher)
     */
    void activate();

    /**
     * After sending a message, the dispatcher can use this method e.g. to
     * clean up its internal state (if it has any) or return pooled resources to
     * whereever it got them during {@link #activate()}.
     * 
     * @see MessageDispatcherFactory#passivate(org.mule.api.endpoint.ImmutableEndpoint, MessageDispatcher)
     */
    void passivate();

    /**
     * Determines whether this dispatcher can be reused after message sending.
     * 
     * @return <code>true</code> if this dispatcher can be reused,
     *         <code>false</code> otherwise (for example when
     *         {@link Disposable#dispose()} has been called because an Exception was
     *         raised)
     */
    boolean validate();

    /**
     * Gets the connector for this dispatcher
     * 
     * @return the connector for this dispatcher
     */
    Connector getConnector();

    /**
     * @return the endpoint which we are dispatching events to
     */
    OutboundEndpoint getEndpoint();
}
