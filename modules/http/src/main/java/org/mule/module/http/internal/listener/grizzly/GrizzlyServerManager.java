/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.grizzly;

import org.mule.api.MuleRuntimeException;
import org.mule.module.http.internal.listener.HttpListenerRegistry;
import org.mule.module.http.internal.listener.HttpServerManager;
import org.mule.module.http.internal.listener.Server;
import org.mule.module.http.internal.listener.ServerAddress;
import org.mule.transport.ssl.TlsContextFactory;
import org.mule.transport.tcp.TcpServerSocketProperties;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.HttpCodecFilter;
import org.glassfish.grizzly.http.HttpServerFilter;
import org.glassfish.grizzly.http.KeepAlive;
import org.glassfish.grizzly.nio.RoundRobinConnectionDistributor;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.ssl.SSLFilter;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrizzlyServerManager implements HttpServerManager
{

    private final GrizzlyHttpsSslDelegateFilter sslFilterDelegate;
    private final TCPNIOTransport transport;
    private final GrizzlyRequestDispatcherFilter requestHandlerFilter;
    private final HttpListenerRegistry httpListenerRegistry;
    private Logger logger = LoggerFactory.getLogger(GrizzlyServerManager.class);
    private Map<ServerAddress, GrizzlyServer> servers = new ConcurrentHashMap<>();

    public GrizzlyServerManager(HttpListenerRegistry httpListenerRegistry, TcpServerSocketProperties serverSocketProperties) throws IOException
    {
        this.httpListenerRegistry = httpListenerRegistry;
        this.requestHandlerFilter = new GrizzlyRequestDispatcherFilter(httpListenerRegistry);
        FilterChainBuilder serverFilterChainBuilder = FilterChainBuilder.stateless();

        serverFilterChainBuilder.add(new TransportFilter());
        sslFilterDelegate = new GrizzlyHttpsSslDelegateFilter();
        serverFilterChainBuilder.add(sslFilterDelegate);
        KeepAlive ka = new KeepAlive();
        ka.setMaxRequestsCount(-1);
        serverFilterChainBuilder.add(new HttpServerFilter(true, HttpCodecFilter.DEFAULT_MAX_HTTP_PACKET_HEADER_SIZE, ka, null));
        serverFilterChainBuilder.add(requestHandlerFilter);

        //Initialize Transport
        TCPNIOTransportBuilder transportBuilder = TCPNIOTransportBuilder.newInstance()
                .setOptimizedForMultiplexing(true)
                .setIOStrategy(SameThreadIOStrategy.getInstance());

        configureServerSocketProperties(transportBuilder, serverSocketProperties);

        transport = transportBuilder.build();

        transport.setNIOChannelDistributor(new RoundRobinConnectionDistributor(transport, true, true));

        // Set filterchain as a Transport Processor
        transport.setProcessor(serverFilterChainBuilder.build());
        transport.start();
    }

    private void configureServerSocketProperties(TCPNIOTransportBuilder transportBuilder, TcpServerSocketProperties serverSocketProperties)
    {
        if (serverSocketProperties.getKeepAlive() != null)
        {
            transportBuilder.setKeepAlive(serverSocketProperties.getKeepAlive());
        }
        if (serverSocketProperties.getLinger() != null)
        {
            transportBuilder.setLinger(serverSocketProperties.getLinger());
        }
        if (serverSocketProperties.getReuseAddress() != null)
        {
            transportBuilder.setReuseAddress(serverSocketProperties.getReuseAddress());
        }
        if (serverSocketProperties.getSendTcpNoDelay() != null)
        {
            transportBuilder.setTcpNoDelay(serverSocketProperties.getSendTcpNoDelay());
        }
        if (serverSocketProperties.getReceiveBacklog() != null)
        {
            transportBuilder.setServerConnectionBackLog(serverSocketProperties.getReceiveBacklog());
        }
        if (serverSocketProperties.getReceiveBufferSize() != null)
        {
            transportBuilder.setReadBufferSize(serverSocketProperties.getReceiveBufferSize());
        }
        if (serverSocketProperties.getSendBufferSize() != null)
        {
            transportBuilder.setWriteBufferSize(serverSocketProperties.getSendBufferSize());
        }
        if (serverSocketProperties.getServerTimeout() != null)
        {
            transportBuilder.setServerSocketSoTimeout(serverSocketProperties.getServerTimeout());
        }
        if (serverSocketProperties.getTimeout() != null)
        {
            transportBuilder.setClientSocketSoTimeout(serverSocketProperties.getTimeout());
        }
    }

    @Override
    public boolean containsServerFor(final ServerAddress serverAddress)
    {
        return servers.containsKey(serverAddress);
    }

    public Server createSslServerFor(TlsContextFactory tlsContextFactory, final ServerAddress serverAddress) throws IOException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Creating https server socket for host %s and path %s", serverAddress.getHost(), serverAddress.getPort());
        }
        if (servers.containsKey(servers))
        {
            throw new IllegalStateException(String.format("Could not create a server for %s since there's already one.", serverAddress));
        }
        final SSLFilter sslFilter = createSslFilter(tlsContextFactory);
        sslFilterDelegate.addSslFilterForAddress(serverAddress, sslFilter);
        final GrizzlyServer grizzlyServer = new GrizzlyServer(serverAddress, transport, httpListenerRegistry);
        servers.put(serverAddress, grizzlyServer);
        return grizzlyServer;
    }

    public Server createServerFor(ServerAddress serverAddress) throws IOException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Creating http server socket for host %s and path %s", serverAddress.getHost(), serverAddress.getPort());
        }
        if (servers.containsKey(servers))
        {
            throw new IllegalStateException(String.format("Could not create a server for %s since there's already one.", serverAddress));
        }
        final GrizzlyServer grizzlyServer = new GrizzlyServer(serverAddress, transport, httpListenerRegistry);
        servers.put(serverAddress, grizzlyServer);
        return grizzlyServer;
    }

    @Override
    public void dispose()
    {
        transport.shutdown();
        servers.clear();
    }

    private SSLFilter createSslFilter(final TlsContextFactory tlsContextFactory)
    {
        try
        {
            final SSLEngineConfigurator serverConfig = new SSLEngineConfigurator(tlsContextFactory.createSslContext(), false, false, false);
            final String[] enabledProtocols = tlsContextFactory.getEnabledProtocols();
            if (enabledProtocols != null)
            {
                serverConfig.setEnabledProtocols(enabledProtocols);
            }
            final String[] enabledCipherSuites = tlsContextFactory.getEnabledCipherSuites();
            if (enabledCipherSuites != null)
            {
                serverConfig.setEnabledCipherSuites(enabledCipherSuites);
            }
            final SSLEngineConfigurator clientConfig = serverConfig.copy().setClientMode(true);
            final SSLFilter sslBaseFilter = new SSLFilter(serverConfig, clientConfig);
            return sslBaseFilter;
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

}
