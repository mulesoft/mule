/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import org.mule.transport.ssl.TlsContextFactory;

import java.io.IOException;

/**
 * A Server managers is in charge to handle all ServerSocket connections and to handle
 * incoming requests to an Execution Service for processing.
 */
public interface HttpServerManager
{

    /**
     * @param serverAddress address of the server
     * @return true if there's a server created for that host and port already, false otherwise.
     */
    boolean containsServerFor(ServerAddress serverAddress);

    /**
     *
     * @param serverAddress address of the server
     * @param usePersistentConnections if true, the connections will be kept open for subsequent requests
     * @param connectionIdleTimeout the amount of milliseconds to keep open an idle connection
     * @return the create Server handler
     * @throws IOException if it was not possible to create the Server. Most likely because the host and port is already in use.
     */
    Server createServerFor(ServerAddress serverAddress, boolean usePersistentConnections, int connectionIdleTimeout) throws IOException;

    /**
     *
     * @param tlsContextFactory
     * @param serverAddress address of the server
     * @param usePersistentConnections if true, the connections will be kept open for subsequent requests
     * @param connectionIdleTimeout the amount of milliseconds to keep open an idle connection
     * @return the create Server handler
     * @throws IOException if it was not possible to create the Server. Most likely because the host and port is already in use.
     */
    Server createSslServerFor(TlsContextFactory tlsContextFactory, ServerAddress serverAddress, boolean usePersistentConnections, int connectionIdleTimeout) throws IOException;

    /**
     *
     * Frees all the resource hold by the server manager. The client is responsible for stopping all the server prior to call dispose.
     */
    void dispose();
}
