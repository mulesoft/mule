/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import org.mule.module.http.internal.listener.async.RequestHandler;
import org.mule.module.http.internal.listener.matcher.ListenerRequestMatcher;

import java.io.IOException;

/**
 * Represents a ServerSocket connection
 */
public interface Server
{

    /**
     * Binds the ServerSocket to the network interface and starts listening for requests
     * @throws IOException if there was a problem binding to the host and port specified
     */
    void start() throws IOException;

    /**
     * Unbinds the ServerSocket to the network interface and stops listening for requests.
     */
    void stop();

    /**
     * @return the server address this server is listening
     */
    ServerAddress getServerAddress();

    /**
     * @return true if the server is currently stopping, false if it's stopped already or if it's not doing stop.
     */
    boolean isStopping();

    /**
     * @return true if the server is stopped, false otherwise
     */
    boolean isStopped();


    RequestHandlerManager addRequestHandler(final ListenerRequestMatcher listenerRequestMatcher, final RequestHandler requestHandler);
}
