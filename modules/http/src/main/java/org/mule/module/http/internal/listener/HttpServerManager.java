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
     * @param host hostname or ip address of a network interface
     * @param port port of the host
     * @return true if there's a server created for that host and port already, false otherwise.
     */
    boolean containsServerFor(String host, int port);

    /**
     *
     * @param host hostname or ip address of a network interface
     * @param port port of the host
     * @param usePersistentConnections if true, the connections will be kept open for subsequent requests
     * @param connectionIdleTimeoutInSeconds the amount of seconds to keep open an idle connection
     * @return the create Server handler
     * @throws IOException if it was not possible to create the Server. Most likely because the host and port is already in use.
     */
    Server createServerFor(String host, int port, boolean usePersistentConnections, int connectionIdleTimeoutInSeconds) throws IOException;

    /**
     *
     * @param tlsContextFactory
     * @param host hostname or ip address of a network interface
     * @param port port of the host
     * @param usePersistentConnections if true, the connections will be kept open for subsequent requests
     * @param connectionIdleTimeoutInSeconds the amount of seconds to keep open an idle connection
     * @return the create Server handler
     * @throws IOException if it was not possible to create the Server. Most likely because the host and port is already in use.
     */
    Server createSslServerFor(TlsContextFactory tlsContextFactory, String host, int port, boolean usePersistentConnections, int connectionIdleTimeoutInSeconds) throws IOException;

    /**
     *
     * Frees all the resource hold by the server manager. The client is responsible for stopping all the server prior to call dispose.
     */
    void dispose();
}
