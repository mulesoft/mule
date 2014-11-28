/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.grizzly;

import org.mule.api.MuleRuntimeException;
import org.mule.api.context.WorkManagerSource;
import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.internal.listener.HttpListenerRegistry;
import org.mule.module.http.internal.listener.HttpServerManager;
import org.mule.module.http.internal.listener.Server;
import org.mule.module.http.internal.listener.ServerAddress;
import org.mule.transport.ssl.TlsContextFactory;
import org.mule.transport.tcp.TcpServerSocketProperties;
import org.mule.util.concurrent.NamedThreadFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.HttpCodecFilter;
import org.glassfish.grizzly.http.HttpServerFilter;
import org.glassfish.grizzly.http.KeepAlive;
import org.glassfish.grizzly.nio.RoundRobinConnectionDistributor;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.ssl.SSLFilter;
import org.glassfish.grizzly.utils.DelayedExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrizzlyServerManager implements HttpServerManager
{

    private static final int MAX_KEEP_ALIVE_REQUESTS = -1;
    private static final String IDLE_TIMEOUT_THREADS_PREFIX_NAME = "HttpIdleConnectionCloser";
    private final GrizzlyAddressDelegateFilter<SSLFilter> sslFilterDelegate;
    private final GrizzlyAddressDelegateFilter<HttpServerFilter> httpServerFilterDelegate;
    private final TCPNIOTransport transport;
    private final GrizzlyRequestDispatcherFilter requestHandlerFilter;
    private final HttpListenerRegistry httpListenerRegistry;
    private final WorkManagerSourceExecutorProvider executorProvider;
    private Logger logger = LoggerFactory.getLogger(GrizzlyServerManager.class);
    private Map<ServerAddress, GrizzlyServer> servers = new ConcurrentHashMap<>();
    private ExecutorService idleTimeoutExecutorService;
    private DelayedExecutor idleTimeoutDelayedExecutor;

    public GrizzlyServerManager(final String appName, HttpListenerRegistry httpListenerRegistry, TcpServerSocketProperties serverSocketProperties) throws IOException
    {
        this.httpListenerRegistry = httpListenerRegistry;
        requestHandlerFilter = new GrizzlyRequestDispatcherFilter(httpListenerRegistry);
        sslFilterDelegate = new GrizzlyAddressDelegateFilter<>();
        httpServerFilterDelegate = new GrizzlyAddressDelegateFilter<>();

        FilterChainBuilder serverFilterChainBuilder = FilterChainBuilder.stateless();
        serverFilterChainBuilder.add(new TransportFilter());
        serverFilterChainBuilder.add(sslFilterDelegate);
        serverFilterChainBuilder.add(httpServerFilterDelegate);
        serverFilterChainBuilder.add(requestHandlerFilter);

        //Initialize Transport
        executorProvider = new WorkManagerSourceExecutorProvider();
        TCPNIOTransportBuilder transportBuilder = TCPNIOTransportBuilder.newInstance()
                .setOptimizedForMultiplexing(true)
                .setIOStrategy(new ExecutorPerServerAddressIOStrategy(executorProvider));

        configureServerSocketProperties(transportBuilder, serverSocketProperties);

        transport = transportBuilder.build();

        transport.setNIOChannelDistributor(new RoundRobinConnectionDistributor(transport, true, true));

        // Set filterchain as a Transport Processor
        transport.setProcessor(serverFilterChainBuilder.build());
        transport.start();

        idleTimeoutExecutorService = Executors.newCachedThreadPool(new NamedThreadFactory(appName + IDLE_TIMEOUT_THREADS_PREFIX_NAME));
        idleTimeoutDelayedExecutor = new DelayedExecutor(idleTimeoutExecutorService);
        idleTimeoutDelayedExecutor.start();
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
        return servers.containsKey(serverAddress) || containsOverlappingServerFor(serverAddress);
    }

    private boolean containsOverlappingServerFor(ServerAddress newServerAddress)
    {
        for (ServerAddress serverAddress : servers.keySet())
        {
            if (serverAddress.overlaps(newServerAddress))
            {
                return true;
            }
        }
        return false;
    }

    public Server createSslServerFor(TlsContextFactory tlsContextFactory, WorkManagerSource workManagerSource, final ServerAddress serverAddress, boolean usePersistentConnections, int connectionIdleTimeout) throws IOException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Creating https server socket for host %s and path %s", serverAddress.getHost(), serverAddress.getPort());
        }
        if (servers.containsKey(serverAddress))
        {
            throw new IllegalStateException(String.format("Could not create a server for %s since there's already one.", serverAddress));
        }
        sslFilterDelegate.addFilterForAddress(serverAddress, createSslFilter(tlsContextFactory));
        httpServerFilterDelegate.addFilterForAddress(serverAddress, createHttpServerFilter(usePersistentConnections, connectionIdleTimeout));
        executorProvider.addExecutor(serverAddress, workManagerSource);
        final GrizzlyServer grizzlyServer = new GrizzlyServer(serverAddress, transport, httpListenerRegistry);
        servers.put(serverAddress, grizzlyServer);
        return grizzlyServer;
    }

    public Server createServerFor(ServerAddress serverAddress, WorkManagerSource workManagerSource, boolean usePersistentConnections, int connectionIdleTimeout) throws IOException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Creating http server socket for host %s and path %s", serverAddress.getHost(), serverAddress.getPort());
        }
        if (servers.containsKey(serverAddress))
        {
            throw new IllegalStateException(String.format("Could not create a server for %s since there's already one.", serverAddress));
        }
        httpServerFilterDelegate.addFilterForAddress(serverAddress, createHttpServerFilter(usePersistentConnections, connectionIdleTimeout));
        executorProvider.addExecutor(serverAddress, workManagerSource);
        final GrizzlyServer grizzlyServer = new GrizzlyServer(serverAddress, transport, httpListenerRegistry);
        servers.put(serverAddress, grizzlyServer);
        return grizzlyServer;
    }

    @Override
    public void dispose()
    {
        transport.shutdown();
        servers.clear();
        idleTimeoutDelayedExecutor.destroy();
        idleTimeoutExecutorService.shutdown();
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
            final SSLFilter sslBaseFilter = new SSLFilter(serverConfig, clientConfig) {
                @Override
                public NextAction handleRead(FilterChainContext ctx) throws IOException
                {
                    ctx.getAttributes().setAttribute(HttpConstants.Protocols.HTTPS, true);
                    return super.handleRead(ctx);
                }
            };
            return sslBaseFilter;
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    private HttpServerFilter createHttpServerFilter(boolean usePersistentConnections, int connectionIdleTimeout)
    {
        KeepAlive ka = null;
        if (usePersistentConnections)
        {
            ka = new KeepAlive();
            ka.setMaxRequestsCount(MAX_KEEP_ALIVE_REQUESTS);
            ka.setIdleTimeoutInSeconds(convertToSeconds(connectionIdleTimeout));
        }
        return new HttpServerFilter(true, HttpCodecFilter.DEFAULT_MAX_HTTP_PACKET_HEADER_SIZE, ka, idleTimeoutDelayedExecutor);
    }

    private int convertToSeconds(int milliseconds)
    {
        return (int) Math.ceil((double) milliseconds / 1000.0);

    }

}
