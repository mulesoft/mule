/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.provider;

import org.mule.umo.lifecycle.Disposable;

/**
 * Combine {@link org.mule.umo.provider.UMOMessageRequesting} with
 * various lifecycle methods for the actual instances doing message sending.
 */
public interface UMOMessageRequester extends Disposable, UMOConnectable, UMOMessageRequesting
{

    /**
     * This method can perform necessary state updates before any of the
     * {@link org.mule.umo.provider.UMOMessageDispatching} methods are invoked.
     *
     * @see {@link org.mule.umo.provider.UMOMessageDispatcherFactory#activate(org.mule.umo.endpoint.UMOImmutableEndpoint, org.mule.umo.provider.UMOMessageDispatcher)}
     */
    void activate();

    /**
     * After receiving a message, the dispatcher can use this method e.g. to
     * clean up its internal state (if it has any) or return pooled resources to
     * whereever it got them during {@link #activate()}.
     *
     * @see {@link org.mule.umo.provider.UMOMessageDispatcherFactory#passivate(org.mule.umo.endpoint.UMOImmutableEndpoint, org.mule.umo.provider.UMOMessageDispatcher)}
     */
    void passivate();

    /**
     * Determines whether this dispatcher can be reused after message receiving.
     *
     * @return <code>true</code> if this dispatcher can be reused,
     *         <code>false</code> otherwise (for example when
     *         {@link org.mule.umo.lifecycle.Disposable#dispose()} has been called because an Exception was
     *         raised)
     */
    boolean validate();

    /**
     * Gets the connector for this dispatcher
     *
     * @return the connector for this dispatcher
     */
    UMOConnector getConnector();

}