/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.grizzly;

import static java.lang.Math.min;
import static java.lang.System.nanoTime;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import org.mule.module.http.internal.listener.HttpListenerRegistry;
import org.mule.module.http.internal.listener.RequestHandlerManager;
import org.mule.module.http.internal.listener.Server;
import org.mule.module.http.internal.listener.ServerAddress;
import org.mule.module.http.internal.listener.async.RequestHandler;
import org.mule.module.http.internal.listener.matcher.ListenerRequestMatcher;

import com.google.common.base.Supplier;

import java.io.IOException;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.ConnectionProbe;
import org.glassfish.grizzly.nio.transport.TCPNIOServerConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrizzlyServer implements Server
{
    private Logger LOGGER = LoggerFactory.getLogger(GrizzlyServer.class);

    private final TCPNIOTransport transport;
    private final ServerAddress serverAddress;
    private final HttpListenerRegistry listenerRegistry;
    private final Supplier<Long> shutdownTimeoutSupplier;
    private TCPNIOServerConnection serverConnection;
    private boolean stopped = true;
    private boolean stopping;
    private boolean shouldWaitConnectionsOnStop;

    private final Object openConnectionsSync = new Object();
    private volatile int openConnections = 0;

    public GrizzlyServer(ServerAddress serverAddress, TCPNIOTransport transport, HttpListenerRegistry listenerRegistry,
                         Supplier<Long> shutdownTimeout)
    {
        this.serverAddress = serverAddress;
        this.transport = transport;
        this.listenerRegistry = listenerRegistry;
        this.shutdownTimeoutSupplier = shutdownTimeout;
    }

    @Override
    public synchronized void start() throws IOException
    {
        transport.getConnectionMonitoringConfig().addProbes(new ConnectionProbe.Adapter()
        {
            @Override
            public void onAcceptEvent(Connection serverConnection, Connection clientConnection)
            {
                synchronized (openConnectionsSync)
                {
                    openConnections += 1;
                }
                clientConnection.getMonitoringConfig().addProbes(new ConnectionProbe.Adapter()
                {
                    @Override
                    public void onCloseEvent(Connection connection)
                    {
                        synchronized (openConnectionsSync)
                        {
                            openConnections -= 1;
                            if (openConnections == 0)
                            {
                                openConnectionsSync.notifyAll();
                            }
                        }
                    }
                });
            }
        });
        serverConnection = transport.bind(serverAddress.getIp(), serverAddress.getPort());
        stopped = false;
    }

    @Override
    public synchronized void stop()
    {
        if (stopped)
        {
            return;
        }
        stopping = true;

        Long shutdownTimeout = shutdownTimeoutSupplier.get();
        final long stopNanos = nanoTime() + MILLISECONDS.toNanos(shutdownTimeout);

        try
        {
            transport.unbind(serverConnection);
            if (shouldWaitConnectionsOnStop && shutdownTimeout != 0)
            {
                synchronized (openConnectionsSync)
                {
                    long remainingMillis = NANOSECONDS.toMillis(stopNanos - nanoTime());
                    while (openConnections != 0 && remainingMillis > 0)
                    {
                        openConnectionsSync.wait(min(remainingMillis, 50));
                        remainingMillis = NANOSECONDS.toMillis(stopNanos - nanoTime());
                    }
                    if (openConnections != 0) {
                        LOGGER.warn("There are still {} open connections on server stop.", openConnections);
                    }
                }
            }
        }
        catch (InterruptedException e)
        {
            currentThread().interrupt();
        }
        finally
        {
            stopped = true;
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

    public void setWaitConnectionsOnStop(boolean value)
    {
        shouldWaitConnectionsOnStop = value;
    }
}
