/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.provider;

import org.mule.umo.lifecycle.Disposable;

/**
 * <code>UMOMessageDispatcher</code> combines {@link UMOMessageDispatching} with
 * various lifecycle methods for the actual instances doing message sending/receiving.
 */
public interface UMOMessageDispatcher extends Disposable, UMOConnectable, UMOMessageDispatching
{

    /**
     * This method can perform necessary state updates before any of the
     * {@link UMOMessageDispatching} methods are invoked.
     * 
     * @see {@link UMOMessageDispatcherFactory#activate(org.mule.umo.endpoint.UMOImmutableEndpoint, UMOMessageDispatcher)}
     */
    void activate();

    /**
     * After sending/receiving a message, the dispatcher can use this method e.g. to
     * clean up its internal state (if it has any) or return pooled resources to
     * whereever it got them during {@link #activate()}.
     * 
     * @see {@link UMOMessageDispatcherFactory#passivate(org.mule.umo.endpoint.UMOImmutableEndpoint, UMOMessageDispatcher)}
     */
    void passivate();

    /**
     * Determines whether this dispatcher can be reused after message
     * sending/receiving.
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
    UMOConnector getConnector();

}
