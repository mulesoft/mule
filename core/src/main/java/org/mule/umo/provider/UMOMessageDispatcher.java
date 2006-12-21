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

import java.io.OutputStream;

/**
 * <code>UMOMessageDispatcher</code> is the interface responsible for distpatching
 * events to a particular transport. It implements the client code necessary to write
 * data to the underlying protocol. The dispatcher also exposes a receive method that
 * allows users to make specific calls to the underlying transport to receive an
 * event.
 */
public interface UMOMessageDispatcher extends Disposable, UMOConnectable, UMOMessageDispatching
{

    /**
     * Gets the connector for this dispatcher
     * 
     * @return the connector for this dispatcher
     */
    UMOConnector getConnector();

    /**
     * Determines if this dispatcher has been disposed. Once disposed a dispatcher
     * cannot be used again
     * 
     * @return true if this dispatcher has been disposed, false otherwise
     */
    boolean isDisposed();

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
    OutputStream getOutputStream(UMOImmutableEndpoint endpoint, UMOMessage message) throws UMOException;

}
