/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTPS;

import org.mule.compatibility.transport.socket.api.TcpServerSocketProperties;
import org.mule.compatibility.transport.socket.internal.DefaultTcpServerSocketProperties;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.NetworkUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.http.api.HttpConstants;
import org.mule.runtime.module.http.api.HttpListenerConnectionManager;
import org.mule.runtime.module.http.api.listener.HttpListenerConfig;
import org.mule.runtime.module.http.internal.HttpParser;
import org.mule.runtime.module.http.internal.listener.matcher.ListenerRequestMatcher;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.RequestHandler;
import org.mule.service.http.api.server.RequestHandlerManager;
import org.mule.service.http.api.server.ServerAddress;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultHttpListenerConfig extends AbstractAnnotatedObject
    implements HttpListenerConfig, Initialisable, MuleContextAware {

  public static final int DEFAULT_MAX_THREADS = 128;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public static final int DEFAULT_CONNECTION_IDLE_TIMEOUT = 30 * 1000;

  private HttpConstants.Protocols protocol = HttpConstants.Protocols.HTTP;
  private String name;
  private String host;
  private Integer port;
  private String basePath;
  private Boolean parseRequest;
  private MuleContext muleContext;
  @Inject
  private HttpListenerConnectionManager connectionManager;
  private TlsContextFactory tlsContext;
  private TcpServerSocketProperties serverSocketProperties = new DefaultTcpServerSocketProperties();
  private boolean started = false;
  private HttpServer server;
  private Scheduler workManager;
  private boolean initialised;

  private boolean usePersistentConnections = true;
  private int connectionIdleTimeout = DEFAULT_CONNECTION_IDLE_TIMEOUT;

  public DefaultHttpListenerConfig() {}

  DefaultHttpListenerConfig(HttpListenerConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setProtocol(HttpConstants.Protocols protocol) {
    this.protocol = protocol;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  public void setTlsContext(TlsContextFactory tlsContext) {
    this.tlsContext = tlsContext;
  }

  public void setServerSocketProperties(TcpServerSocketProperties serverSocketProperties) {
    this.serverSocketProperties = serverSocketProperties;
  }

  public void setParseRequest(Boolean parseRequest) {
    this.parseRequest = parseRequest;
  }

  public ListenerPath getFullListenerPath(String listenerPath) {
    Preconditions.checkArgument(listenerPath.startsWith("/"), "listenerPath must start with /");
    return new ListenerPath(basePath, listenerPath);
  }

  @Override
  public synchronized void initialise() throws InitialisationException {
    if (initialised) {
      return;
    }
    basePath = HttpParser.sanitizePathWithStartSlash(this.basePath);

    if (port == null) {
      port = protocol.getDefaultPort();
    }

    if (protocol.equals(HTTP) && tlsContext != null) {
      throw new InitialisationException(CoreMessages.createStaticMessage("TlsContext cannot be configured with protocol HTTP. "
          + "If you defined a tls:context element in your listener-config then you must set protocol=\"HTTPS\""), this);
    }
    if (protocol.equals(HTTPS) && tlsContext == null) {
      throw new InitialisationException(CoreMessages
          .createStaticMessage("Configured protocol is HTTPS but there's no TlsContext configured"), this);
    }
    if (tlsContext != null && !tlsContext.isKeyStoreConfigured()) {
      throw new InitialisationException(CoreMessages.createStaticMessage("KeyStore must be configured for server side SSL"),
                                        this);
    }

    verifyConnectionsParameters();


    ServerAddress serverAddress;

    try {
      serverAddress = createServerAddress();
    } catch (UnknownHostException e) {
      throw new InitialisationException(CoreMessages.createStaticMessage("Cannot resolve host %s", host), e, this);
    }

    if (tlsContext == null) {
      server = connectionManager.createServer(serverAddress, () -> workManager, usePersistentConnections,
                                              connectionIdleTimeout);
    } else {
      LifecycleUtils.initialiseIfNeeded(tlsContext);
      server = connectionManager.createSslServer(serverAddress, () -> workManager, tlsContext, usePersistentConnections,
                                                 connectionIdleTimeout);
    }
    initialised = true;
  }

  private void verifyConnectionsParameters() throws InitialisationException {
    if (!usePersistentConnections) {
      connectionIdleTimeout = 0;
    }
  }

  /**
   * Creates the server address object with the IP and port that this config should bind to.
   */
  private ServerAddress createServerAddress() throws UnknownHostException {
    return new DefaultServerAddress(NetworkUtils.getLocalHostIp(host), port);
  }

  @Override
  public void setMuleContext(final MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  public RequestHandlerManager addRequestHandler(ListenerRequestMatcher requestMatcher, RequestHandler requestHandler)
      throws IOException {
    return server.addRequestHandler(requestMatcher, requestHandler);
  }

  public Boolean resolveParseRequest(Boolean listenerParseRequest) {
    return listenerParseRequest != null ? listenerParseRequest : (parseRequest != null ? parseRequest : true);
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public TlsContextFactory getTlsContext() {
    return tlsContext;
  }

  @Override
  public synchronized void start() throws MuleException {
    if (started) {
      return;
    }
    try {
      workManager = muleContext.getSchedulerService().cpuLightScheduler();
      server.start();
    } catch (IOException e) {
      throw new DefaultMuleException(e);
    }
    started = true;
    logger.info("Listening for requests on " + listenerUrl());
  }

  @Override
  public boolean hasTlsConfig() {
    return this.tlsContext != null;
  }

  @Override
  public synchronized void stop() throws MuleException {
    if (started) {
      try {
        workManager.stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS);
      } catch (Exception e) {
        logger.warn("Failure shutting down work manager " + e.getMessage());
        if (logger.isDebugEnabled()) {
          logger.debug(e.getMessage(), e);
        }
      } finally {
        workManager = null;
      }
      server.stop();
      started = false;
      logger.info("Stopped listener on " + listenerUrl());
    }
  }

  private String listenerUrl() {
    return String.format("%s://%s:%d%s", protocol.getScheme(), getHost(), getPort(), StringUtils.defaultString(basePath));
  }

  @Override
  public String getName() {
    return name;
  }

  Executor getWorkManager() {
    return workManager;
  }

  public void setUsePersistentConnections(boolean usePersistentConnections) {
    this.usePersistentConnections = usePersistentConnections;
  }

  public void setConnectionIdleTimeout(int connectionIdleTimeout) {
    this.connectionIdleTimeout = connectionIdleTimeout;
  }

  public boolean isStarted() {
    return started;
  }
}
