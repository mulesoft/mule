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

import org.glassfish.grizzly.nio.transport.TCPNIOConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOServerConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.CloseListener;
import org.glassfish.grizzly.CloseType;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.ConnectionProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrizzlyServer implements Server
{
    public static final Logger logger = LoggerFactory.getLogger(GrizzlyServer.class);

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
        serverConnection.getMonitoringConfig().addProbes(new CloseAcceptedConnectionsOnServerCloseProbe());
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

    private static class CloseAcceptedConnectionsOnServerCloseProbe extends ConnectionProbe.Adapter
    {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onAcceptEvent(Connection serverConnection, Connection clientConnection)
        {
            final CloseAcceptedConnectionOnServerClose callback = new CloseAcceptedConnectionOnServerClose(clientConnection);
            serverConnection.addCloseListener(callback);
            clientConnection.addCloseListener(new RemoveCloseListenerOnClientClosed(serverConnection, callback));
        }
    }

    private static class CloseAcceptedConnectionOnServerClose implements CloseListener<TCPNIOServerConnection, CloseType>
    {

        private final Connection acceptedConnection;

        private CloseAcceptedConnectionOnServerClose(Connection acceptedConnection)
        {
            this.acceptedConnection = acceptedConnection;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onClosed(TCPNIOServerConnection closeable, CloseType type) throws IOException
        {
            acceptedConnection.closeSilently();
        }
    }

    private static class RemoveCloseListenerOnClientClosed implements CloseListener<TCPNIOConnection, CloseType>
    {

        private CloseAcceptedConnectionOnServerClose callbackToRemove;
        private Connection serverConnection;

        private RemoveCloseListenerOnClientClosed(Connection serverConnection,
                                                  CloseAcceptedConnectionOnServerClose callbackToRemove)
        {
            this.serverConnection = serverConnection;
            this.callbackToRemove = callbackToRemove;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onClosed(TCPNIOConnection closeable, CloseType type) throws IOException
        {
            if (serverConnection.isOpen())
            {
                serverConnection.removeCloseListener(callbackToRemove);
            }
        }
    }
}
