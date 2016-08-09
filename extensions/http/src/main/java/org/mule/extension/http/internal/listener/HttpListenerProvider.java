/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.listener;

import static java.lang.String.format;
import static org.mule.extension.http.internal.HttpConnector.OTHER_SETTINGS;
import static org.mule.extension.http.internal.HttpConnector.TLS;
import static org.mule.extension.http.internal.HttpConnector.TLS_CONFIGURATION;
import static org.mule.extension.http.internal.HttpConnector.URL_CONFIGURATION;
import static org.mule.runtime.api.connection.ConnectionExceptionCode.UNKNOWN;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.core.api.config.ThreadingProfile.DEFAULT_THREADING_PROFILE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.concurrent.ThreadNameHelper.getPrefix;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTPS;
import org.mule.extension.http.api.server.HttpListenerConnectionManager;
import org.mule.extension.http.internal.listener.server.HttpServerConfiguration;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.context.WorkManagerSource;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.config.MutableThreadingProfile;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.module.http.api.HttpConstants;
import org.mule.runtime.module.http.internal.listener.Server;
import org.mule.runtime.module.http.internal.listener.ServerAddress;

import java.io.IOException;

import javax.inject.Inject;

/**
 * Connection provider for a {@link HttpListener}, handles the creation of {@link Server} instances.
 *
 * @since 4.0
 */
@Alias("listener")
public class HttpListenerProvider implements CachedConnectionProvider<Server>, Initialisable, Startable, Stoppable {

  private static final int DEFAULT_MAX_THREADS = 128;

  @ConfigName
  private String configName;

  /**
   * Host where the requests will be sent.
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  @Placement(group = URL_CONFIGURATION, order = 2)
  private String host;

  /**
   * Port where the requests will be received. If the protocol attribute is HTTP (default) then the default value is 80, if the
   * protocol attribute is HTTPS then the default value is 443.
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  @Placement(group = URL_CONFIGURATION, order = 3)
  private Integer port;

  /**
   * Protocol to use for communication. Valid values are HTTP and HTTPS. Default value is HTTP. When using HTTPS the HTTP
   * communication is going to be secured using TLS / SSL. If HTTPS was configured as protocol then the user needs to configure at
   * least the keystore in the tls:context child element of this listener-config.
   */
  @Parameter
  @Optional(defaultValue = "HTTP")
  @Expression(NOT_SUPPORTED)
  @Placement(group = URL_CONFIGURATION, order = 1)
  private HttpConstants.Protocols protocol;

  /**
   * Reference to a TLS config element. This will enable HTTPS for this config.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  @DisplayName(TLS_CONFIGURATION)
  @Placement(group = TLS_CONFIGURATION, tab = TLS)
  private TlsContextFactory tlsContext;

  /**
   * The number of milliseconds that a connection can remain idle before it is closed. The value of this attribute is only used
   * when persistent connections are enabled.
   */
  @Parameter
  @Optional(defaultValue = "30000")
  @Expression(NOT_SUPPORTED)
  @Placement(group = OTHER_SETTINGS)
  private Integer connectionIdleTimeout;

  /**
   * If false, each connection will be closed after the first request is completed.
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Expression(NOT_SUPPORTED)
  @Placement(group = OTHER_SETTINGS)
  private Boolean usePersistentConnections;

  @Inject
  private HttpListenerConnectionManager connectionManager;

  @Inject
  private MuleContext muleContext;

  // TODO: MULE-9320 Define threading model for message sources in Mule 4 - This should be a parameter if nothing changes
  private ThreadingProfile workerThreadingProfile;
  private WorkManager workManager;
  private Server server;

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(connectionManager);

    if (port == null) {
      port = protocol.getDefaultPort();
    }

    if (protocol.equals(HTTP) && tlsContext != null) {
      throw new InitialisationException(createStaticMessage("TlsContext cannot be configured with protocol HTTP. "
          + "If you defined a tls:context element in your listener-config then you must set protocol=\"HTTPS\""), this);
    }
    if (protocol.equals(HTTPS) && tlsContext == null) {
      throw new InitialisationException(createStaticMessage("Configured protocol is HTTPS but there's no TlsContext configured"),
                                        this);
    }
    if (tlsContext != null && !tlsContext.isKeyStoreConfigured()) {
      throw new InitialisationException(createStaticMessage("KeyStore must be configured for server side SSL"), this);
    }

    if (tlsContext != null) {
      initialiseIfNeeded(tlsContext);
    }

    verifyConnectionsParameters();

    // TODO: MULE-9320 Define threading model for message sources in Mule 4 - Analyse whether this can be avoided
    workerThreadingProfile = new MutableThreadingProfile(DEFAULT_THREADING_PROFILE);
    workerThreadingProfile.setMaxThreadsActive(DEFAULT_MAX_THREADS);

    HttpServerConfiguration serverConfiguration = new HttpServerConfiguration.Builder().setHost(host).setPort(port)
        .setTlsContextFactory(tlsContext).setUsePersistentConnections(usePersistentConnections)
        .setConnectionIdleTimeout(connectionIdleTimeout).setWorkManagerSource(createWorkManagerSource(workManager)).build();
    try {
      server = connectionManager.create(serverConfiguration);
    } catch (ConnectionException e) {
      throw new InitialisationException(createStaticMessage("Could not create HTTP server"), this);
    }
  }

  @Override
  public void start() throws MuleException {
    try {
      workManager = createWorkManager(configName);
      workManager.start();
      server.start();
    } catch (IOException e) {
      throw new DefaultMuleException(new ConnectionException("Could not start HTTP server", e));
    }
  }

  @Override
  public void stop() throws MuleException {
    try {
      server.stop();
    } finally {
      try {
        workManager.dispose();
      } finally {
        workManager = null;
      }
    }
  }

  @Override
  public Server connect() throws ConnectionException {
    return server;
  }

  @Override
  public void disconnect(Server server) {
    // server could be shared with other listeners, do nothing
  }

  @Override
  public ConnectionValidationResult validate(Server server) {
    if (server.isStopped() || server.isStopping()) {
      ServerAddress serverAddress = server.getServerAddress();
      return failure(format("Server on host %s and port %s is stopped.", serverAddress.getIp(), serverAddress.getPort()), UNKNOWN,
                     new ConnectionException("Server stopped."));
    } else {
      return ConnectionValidationResult.success();
    }
  }

  private void verifyConnectionsParameters() throws InitialisationException {
    if (!usePersistentConnections) {
      connectionIdleTimeout = 0;
    }
  }

  private WorkManager createWorkManager(String name) {
    final WorkManager workManager =
        workerThreadingProfile.createWorkManager(format("%s%s.%s", getPrefix(muleContext), name, "worker"),
                                                 muleContext.getConfiguration().getShutdownTimeout());
    if (workManager instanceof MuleContextAware) {
      ((MuleContextAware) workManager).setMuleContext(muleContext);
    }
    return workManager;
  }

  private WorkManagerSource createWorkManagerSource(WorkManager workManager) {
    return () -> workManager;
  }
}
