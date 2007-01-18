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

import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Registerable;

import java.io.OutputStream;

/**
 * <code>UMOMessageDispatcher</code> combines {@link UMOMessageDispatching} with
 * various lifecycle methods for the actual instances doing message sending/receiving.
 */
public interface UMOMessageDispatcher extends Registerable, Disposable, UMOConnectable, UMOMessageDispatching
{

    /**
     * This method can perform necessary state updates before any of the
     * {@link UMOMessageDispatching} methods are invoked.
     * 
     * @see {@link UMOMessageDispatcherFactory#activate(UMOImmutableEndpoint, UMOMessageDispatcher)}
     */
    public void activate();

    /**
     * After sending/receiving a message, the dispatcher can use this method e.g. to
     * clean up its internal state (if it has any) or return pooled resources to
     * whereever it got them during {@link #activate()}.
     * 
     * @see {@link UMOMessageDispatcherFactory#passivate(UMOImmutableEndpoint, UMOMessageDispatcher)}
     */
    public void passivate();

    /**
     * Determines whether this dispatcher can be reused after message
     * sending/receiving.
     * 
     * @return <code>true</code> if this dispatcher can be reused,
     *         <code>false</code> otherwise (for example when
     *         {@link Disposable#dispose()} has been called because an Exception was
     *         raised)
     */
    public boolean validate();

    /**
     * Gets the connector for this dispatcher
     * 
     * @return the connector for this dispatcher
     */
    UMOConnector getConnector();

    /**
     * Well get the output stream (if any) for this type of transport. Typically this
     * will be called only when Streaming is being used on an outbound endpoint
     * 
     * @param endpoint the endpoint that releates to this Dispatcher
     * @param message the current message being processed
     * @return the output stream to use for this request or null if the transport
     *         does not support streaming
     * @throws UMOException
     */
    // TODO HH: this one needs to move to the connector, and I can already see more
    // trouble with it than I want to think of..
    OutputStream getOutputStream(UMOImmutableEndpoint endpoint, UMOMessage message) throws UMOException;

}
