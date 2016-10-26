/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.listener.grizzly;

import static java.lang.Integer.valueOf;
import static java.lang.System.getProperty;
import static org.glassfish.grizzly.http.HttpCodecFilter.DEFAULT_MAX_HTTP_PACKET_HEADER_SIZE;
import static org.mule.runtime.core.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.module.http.internal.HttpMessageLogger.LoggerType.LISTENER;
import org.mule.extension.socket.api.socket.tcp.TcpServerSocketProperties;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.context.WorkManagerSource;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.concurrent.NamedThreadFactory;
import org.mule.runtime.module.http.internal.HttpMessageLogger;
import org.mule.runtime.module.http.internal.listener.HttpListenerRegistry;
import org.mule.runtime.module.http.internal.listener.HttpServerManager;
import org.mule.runtime.module.http.internal.listener.Server;
import org.mule.runtime.module.http.internal.listener.ServerAddress;
import org.mule.runtime.module.http.internal.listener.grizzly.ExecutorPerServerAddressIOStrategy;
import org.mule.runtime.module.http.internal.listener.grizzly.GrizzlyAddressDelegateFilter;
import org.mule.runtime.module.http.internal.listener.grizzly.GrizzlyServer;
import org.mule.runtime.module.http.internal.listener.grizzly.MuleSslFilter;
import org.mule.runtime.module.http.internal.listener.grizzly.WorkManagerSourceExecutorProvider;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.HttpServerFilter;
import org.glassfish.grizzly.http.KeepAlive;
import org.glassfish.grizzly.nio.RoundRobinConnectionDistributor;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.ssl.SSLFilter;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.grizzly.utils.DelayedExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrizzlyServerManager implements HttpServerManager {

  // Defines the maximum size in bytes accepted for the http request header section (request line + headers)
  public static final String MAXIMUM_HEADER_SECTION_SIZE_PROPERTY_KEY = SYSTEM_PROPERTY_PREFIX + "http.headerSectionSize";
  private static final int MAX_KEEP_ALIVE_REQUESTS = -1;
  private static final String IDLE_TIMEOUT_THREADS_PREFIX_NAME = ".HttpIdleConnectionCloser";
  private static final String LISTENER_WORKER_THREAD_NAME_SUFFIX = ".worker";
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
  private boolean transportStarted;

  public GrizzlyServerManager(String threadNamePrefix, HttpListenerRegistry httpListenerRegistry,
                              TcpServerSocketProperties serverSocketProperties)
      throws IOException {
    this.httpListenerRegistry = httpListenerRegistry;
    requestHandlerFilter = new GrizzlyRequestDispatcherFilter(httpListenerRegistry);
    sslFilterDelegate = new GrizzlyAddressDelegateFilter<>();
    httpServerFilterDelegate = new GrizzlyAddressDelegateFilter<>();

    FilterChainBuilder serverFilterChainBuilder = FilterChainBuilder.stateless();
    serverFilterChainBuilder.add(new TransportFilter());
    serverFilterChainBuilder.add(sslFilterDelegate);
    serverFilterChainBuilder.add(httpServerFilterDelegate);
    serverFilterChainBuilder.add(requestHandlerFilter);

    // Initialize Transport
    executorProvider = new WorkManagerSourceExecutorProvider();
    TCPNIOTransportBuilder transportBuilder = TCPNIOTransportBuilder.newInstance().setOptimizedForMultiplexing(true)
        .setIOStrategy(new ExecutorPerServerAddressIOStrategy(executorProvider));

    configureServerSocketProperties(transportBuilder, serverSocketProperties);

    transport = transportBuilder.build();

    transport.setNIOChannelDistributor(new RoundRobinConnectionDistributor(transport, true, true));

    transport.getWorkerThreadPoolConfig().setPoolName(threadNamePrefix + LISTENER_WORKER_THREAD_NAME_SUFFIX);

    // No kernel thread pool config is set in the transport at this point. Need to set one to define the pool name.
    transport.setKernelThreadPoolConfig(ThreadPoolConfig.defaultConfig().setCorePoolSize(transport.getSelectorRunnersCount())
        .setMaxPoolSize(transport.getSelectorRunnersCount()).setPoolName(threadNamePrefix));

    // Set filterchain as a Transport Processor
    transport.setProcessor(serverFilterChainBuilder.build());

    idleTimeoutExecutorService =
        Executors.newCachedThreadPool(new NamedThreadFactory(threadNamePrefix + IDLE_TIMEOUT_THREADS_PREFIX_NAME));
    idleTimeoutDelayedExecutor = new DelayedExecutor(idleTimeoutExecutorService);

  }

  private void configureServerSocketProperties(TCPNIOTransportBuilder transportBuilder,
                                               TcpServerSocketProperties serverSocketProperties) {
    if (serverSocketProperties.getKeepAlive() != null) {
      transportBuilder.setKeepAlive(serverSocketProperties.getKeepAlive());
    }
    if (serverSocketProperties.getLinger() != null) {
      transportBuilder.setLinger(serverSocketProperties.getLinger());
    }

    if (serverSocketProperties.getReceiveBufferSize() != null) {
      transportBuilder.setReadBufferSize(serverSocketProperties.getReceiveBufferSize());
    }

    if (serverSocketProperties.getSendBufferSize() != null) {
      transportBuilder.setWriteBufferSize(serverSocketProperties.getSendBufferSize());
    }

    if (serverSocketProperties.getClientTimeout() != null) {
      transportBuilder.setClientSocketSoTimeout(serverSocketProperties.getClientTimeout());
    }

    if (serverSocketProperties.getServerTimeout() != null) {
      transportBuilder.setServerSocketSoTimeout(serverSocketProperties.getServerTimeout());
    }

    transportBuilder.setReuseAddress(serverSocketProperties.getReuseAddress());
    transportBuilder.setTcpNoDelay(serverSocketProperties.getSendTcpNoDelay());
    transportBuilder.setServerConnectionBackLog(serverSocketProperties.getReceiveBacklog());
  }

  /**
   * Starts the transport and the {@code idleTimeoutExecutorService} if not started. This is because they should be started lazily
   * when the first server is registered (otherwise there will be Grizzly threads even if there is no listener-config in the app).
   */
  private void startTransportIfNotStarted() throws IOException {
    if (!transportStarted) {
      transportStarted = true;
      transport.start();
      idleTimeoutDelayedExecutor.start();
    }
  }

  @Override
  public boolean containsServerFor(final ServerAddress serverAddress) {
    return servers.containsKey(serverAddress) || containsOverlappingServerFor(serverAddress);
  }

  private boolean containsOverlappingServerFor(ServerAddress newServerAddress) {
    for (ServerAddress serverAddress : servers.keySet()) {
      if (serverAddress.overlaps(newServerAddress)) {
        return true;
      }
    }
    return false;
  }

  public Server createSslServerFor(TlsContextFactory tlsContextFactory, WorkManagerSource workManagerSource,
                                   final ServerAddress serverAddress, boolean usePersistentConnections, int connectionIdleTimeout)
      throws IOException {
    if (logger.isDebugEnabled()) {
      logger.debug("Creating https server socket for ip {} and port {}", serverAddress.getIp(), serverAddress.getPort());
    }
    if (servers.containsKey(serverAddress)) {
      throw new IllegalStateException(String.format("Could not create a server for %s since there's already one.",
                                                    serverAddress));
    }
    startTransportIfNotStarted();
    sslFilterDelegate.addFilterForAddress(serverAddress, createSslFilter(tlsContextFactory));
    httpServerFilterDelegate.addFilterForAddress(serverAddress,
                                                 createHttpServerFilter(usePersistentConnections, connectionIdleTimeout));
    executorProvider.addExecutor(serverAddress, workManagerSource);
    final GrizzlyServer grizzlyServer = new GrizzlyServer(serverAddress, transport, httpListenerRegistry);
    servers.put(serverAddress, grizzlyServer);
    return grizzlyServer;
  }

  public Server createServerFor(ServerAddress serverAddress, WorkManagerSource workManagerSource,
                                boolean usePersistentConnections, int connectionIdleTimeout)
      throws IOException {
    if (logger.isDebugEnabled()) {
      logger.debug("Creating http server socket for ip {} and port {}", serverAddress.getIp(), serverAddress.getPort());
    }
    if (servers.containsKey(serverAddress)) {
      throw new IllegalStateException(String.format("Could not create a server for %s since there's already one.",
                                                    serverAddress));
    }
    startTransportIfNotStarted();
    httpServerFilterDelegate.addFilterForAddress(serverAddress,
                                                 createHttpServerFilter(usePersistentConnections, connectionIdleTimeout));
    executorProvider.addExecutor(serverAddress, workManagerSource);
    final GrizzlyServer grizzlyServer = new GrizzlyServer(serverAddress, transport, httpListenerRegistry);
    servers.put(serverAddress, grizzlyServer);
    return grizzlyServer;
  }

  @Override
  public void dispose() {
    if (transportStarted) {
      transport.shutdown();
      servers.clear();
      idleTimeoutDelayedExecutor.destroy();
      idleTimeoutExecutorService.shutdown();
    }
  }

  private SSLFilter createSslFilter(final TlsContextFactory tlsContextFactory) {
    try {
      boolean clientAuth = tlsContextFactory.isTrustStoreConfigured();
      final SSLEngineConfigurator serverConfig =
          new SSLEngineConfigurator(tlsContextFactory.createSslContext(), false, clientAuth, false);
      final String[] enabledProtocols = tlsContextFactory.getEnabledProtocols();
      if (enabledProtocols != null) {
        serverConfig.setEnabledProtocols(enabledProtocols);
      }
      final String[] enabledCipherSuites = tlsContextFactory.getEnabledCipherSuites();
      if (enabledCipherSuites != null) {
        serverConfig.setEnabledCipherSuites(enabledCipherSuites);
      }
      final SSLEngineConfigurator clientConfig = serverConfig.copy().setClientMode(true);
      return new MuleSslFilter(serverConfig, clientConfig);
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  private HttpServerFilter createHttpServerFilter(boolean usePersistentConnections, int connectionIdleTimeout) {
    KeepAlive ka = null;
    if (usePersistentConnections) {
      ka = new KeepAlive();
      ka.setMaxRequestsCount(MAX_KEEP_ALIVE_REQUESTS);
      ka.setIdleTimeoutInSeconds(convertToSeconds(connectionIdleTimeout));
    }
    HttpServerFilter httpServerFilter =
        new HttpServerFilter(true, retrieveMaximumHeaderSectionSize(), ka, idleTimeoutDelayedExecutor);
    httpServerFilter.getMonitoringConfig().addProbes(new HttpMessageLogger(LISTENER));
    httpServerFilter.setAllowPayloadForUndefinedHttpMethods(true);
    return httpServerFilter;
  }

  private int retrieveMaximumHeaderSectionSize() {
    try {
      return valueOf(getProperty(MAXIMUM_HEADER_SECTION_SIZE_PROPERTY_KEY, String.valueOf(DEFAULT_MAX_HTTP_PACKET_HEADER_SIZE)));
    } catch (NumberFormatException e) {
      throw new MuleRuntimeException(CoreMessages.createStaticMessage(String.format("Invalid value %s for %s configuration",
                                                                                    getProperty(MAXIMUM_HEADER_SECTION_SIZE_PROPERTY_KEY),
                                                                                    MAXIMUM_HEADER_SECTION_SIZE_PROPERTY_KEY)),
                                     e);
    }
  }

  private int convertToSeconds(int milliseconds) {
    return (int) Math.ceil((double) milliseconds / 1000.0);

  }

}
