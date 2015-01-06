/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.grizzly;

import org.mule.module.http.internal.listener.HttpListenerRegistry;
import org.mule.module.http.internal.listener.RequestHandlerManager;
import org.mule.module.http.internal.listener.Server;
import org.mule.module.http.internal.listener.ServerAddress;
import org.mule.module.http.internal.listener.async.RequestHandler;
import org.mule.module.http.internal.listener.matcher.ListenerRequestMatcher;

import java.io.IOException;

import org.glassfish.grizzly.nio.transport.TCPNIOServerConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;

public class GrizzlyServer implements Server
{

    private final TCPNIOTransport transport;
    private final ServerAddress serverAddress;
    private final HttpListenerRegistry listenerRegistry;
    private TCPNIOServerConnection serverConnection;
    private boolean stopped = true;
    private boolean stopping;

    public GrizzlyServer(ServerAddress serverAddress, TCPNIOTransport transport, HttpListenerRegistry listenerRegistry)
    {
        this.serverAddress = serverAddress;
        this.transport = transport;
        this.listenerRegistry = listenerRegistry;
    }

    @Override
    public synchronized void start() throws IOException
    {
        serverConnection = transport.bind(serverAddress.getIp(), serverAddress.getPort());
        stopped = false;
    }

    @Override
    public synchronized void stop()
    {
        stopping = true;
        try
        {
            transport.unbind(serverConnection);
        }
        finally
        {
            stopping = false;
        }
    }

    @Override
    public ServerAddress getServerAddress()
    {
        return serverAddress;
    }

    @Override
    public boolean isStopping()
    {
        return stopping;
    }

    @Override
    public boolean isStopped()
    {
        return stopped;
    }

    @Override
    public RequestHandlerManager addRequestHandler(ListenerRequestMatcher listenerRequestMatcher, RequestHandler requestHandler)
    {
        return this.listenerRegistry.addRequestHandler(this, requestHandler, listenerRequestMatcher);
    }
}
