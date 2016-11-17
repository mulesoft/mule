/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request.validator;

import static java.lang.String.format;
import static org.mule.extension.http.internal.HttpConnectorConstants.AUTHENTICATION;
import static org.mule.extension.http.internal.HttpConnectorConstants.TLS;
import static org.mule.extension.http.internal.HttpConnectorConstants.TLS_CONFIGURATION;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.concurrent.ThreadNameHelper.getPrefix;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.CONNECTION;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTPS;
import org.mule.extension.http.api.request.authentication.HttpAuthentication;
import org.mule.extension.http.api.request.client.HttpClient;
import org.mule.extension.http.api.request.proxy.ProxyConfig;
import org.mule.extension.http.internal.request.client.DefaultUriParameters;
import org.mule.extension.http.internal.request.client.HttpClientConfiguration;
import org.mule.extension.http.internal.request.client.HttpClientFactory;
import org.mule.extension.http.internal.request.grizzly.GrizzlyHttpClient;
import org.mule.extension.socket.api.socket.tcp.TcpClientSocketProperties;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.tls.TlsContextFactoryBuilder;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.module.http.api.HttpConstants;
import org.mule.runtime.module.tls.api.DefaultTlsContextFactoryBuilder;

import java.util.function.Function;

import javax.inject.Inject;

/**
 * Connection provider for a HTTP request, handles the creation of {@link HttpClient} instances.
 *
 * @since 4.0
 */
@Alias("request")
public class HttpRequesterProvider implements CachedConnectionProvider<HttpClient>, Initialisable {

  private static final int UNLIMITED_CONNECTIONS = -1;
  private static final String OBJECT_HTTP_CLIENT_FACTORY = "_httpClientFactory";
  private static final String THREAD_NAME_PREFIX_PATTERN = "%shttp.requester.%s";

  @Inject
  private MuleContext muleContext;

  @ConfigName
  private String configName;

  /**
   * Protocol to use for communication. Valid values are HTTP and HTTPS. Default value is HTTP. When using HTTPS the HTTP
   * communication is going to be secured using TLS / SSL. If HTTPS was configured as protocol then the user can customize the
   * tls/ssl configuration by defining the tls:context child element of this listener-config. If not tls:context is defined then
   * the default JVM certificates are going to be used to establish communication.
   */
  @Parameter
  @Optional(defaultValue = "HTTP")
  @Expression(NOT_SUPPORTED)
  @Summary("Protocol to use for communication. Valid values are HTTP and HTTPS")
  @Placement(group = CONNECTION, order = 1)
  private HttpConstants.Protocols protocol;

  /**
   * Host where the requests will be sent.
   */
  @Parameter
  @Optional
  @Placement(group = CONNECTION, order = 2)
  private Function<Event, String> host;

  /**
   * Port where the requests will be sent. If the protocol attribute is HTTP (default) then the default value is 80, if the
   * protocol attribute is HTTPS then the default value is 443.
   */
  @Parameter
  @Optional
  @Placement(group = CONNECTION, order = 3)
  private Function<Event, Integer> port;

  /**
   * Reference to a TLS config element. This will enable HTTPS for this config.
   */
  @Parameter
  @Optional
  @DisplayName(TLS_CONFIGURATION)
  @Placement(tab = TLS, group = TLS_CONFIGURATION)
  private TlsContextFactory tlsContextFactory;

  /**
   * Reusable configuration element for outbound connections through a proxy. A proxy element must define a host name and a port
   * attributes, and optionally can define a username and a password.
   */
  @Parameter
  @Optional
  @Summary("Reusable configuration element for outbound connections through a proxy")
  @Placement(tab = "Proxy")
  private ProxyConfig proxyConfig;

  /**
   * If false, each connection will be closed after the first request is completed.
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Expression(NOT_SUPPORTED)
  @Placement(tab = ADVANCED, group = CONNECTION, order = 1)
  private Boolean usePersistentConnections;

  /**
   * The maximum number of outbound connections that will be kept open at the same time. By default the number of connections is
   * unlimited.
   */
  @Parameter
  @Optional(defaultValue = "-1")
  @Expression(NOT_SUPPORTED)
  @Placement(tab = ADVANCED, group = CONNECTION, order = 2)
  private Integer maxConnections;

  /**
   * The number of milliseconds that a connection can remain idle before it is closed. The value of this attribute is only used
   * when persistent connections are enabled.
   */
  @Parameter
  @Optional(defaultValue = "30000")
  @Expression(NOT_SUPPORTED)
  @Placement(tab = ADVANCED, group = CONNECTION, order = 3)
  private Integer connectionIdleTimeout;

  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  @Placement(tab = ADVANCED, group = CONNECTION, order = 4)
  private TcpClientSocketProperties clientSocketProperties;

  /**
   * Authentication method to use for the HTTP request.
   */
  @Parameter
  @Optional
  @Placement(tab = AUTHENTICATION)
  private HttpAuthentication authentication;

  @Inject
  @DefaultTlsContextFactoryBuilder
  private TlsContextFactoryBuilder defaultTlsContextFactoryBuilder;

  private HttpClientFactory httpClientFactory;

  @Override
  public ConnectionValidationResult validate(HttpClient httpClient) {
    return ConnectionValidationResult.success();
  }

  @Override
  public void initialise() throws InitialisationException {
    if (port == null) {
      port = muleEvent -> protocol.getDefaultPort();
    }

    if (protocol.equals(HTTP) && tlsContextFactory != null) {
      throw new InitialisationException(createStaticMessage("TlsContext cannot be configured with protocol HTTP, "
          + "when using tls:context you must set attribute protocol=\"HTTPS\""), this);
    }

    if (protocol.equals(HTTPS) && tlsContextFactory == null) {
      // MULE-9480
      initialiseIfNeeded(defaultTlsContextFactoryBuilder);
      tlsContextFactory = defaultTlsContextFactoryBuilder.buildDefault();
    }
    if (tlsContextFactory != null) {
      initialiseIfNeeded(tlsContextFactory);
    }
    if (authentication != null) {
      initialiseIfNeeded(authentication);
    }

    verifyConnectionsParameters();

    httpClientFactory = muleContext.getRegistry().get(OBJECT_HTTP_CLIENT_FACTORY);
  }

  private void verifyConnectionsParameters() throws InitialisationException {
    if (maxConnections < UNLIMITED_CONNECTIONS || maxConnections == 0) {
      throw new InitialisationException(createStaticMessage("The maxConnections parameter only allows positive values or -1 for unlimited concurrent connections."),
                                        this);
    }

    if (!usePersistentConnections) {
      connectionIdleTimeout = 0;
    }
  }

  @Override
  public HttpClient connect() throws ConnectionException {
    String threadNamePrefix = format(THREAD_NAME_PREFIX_PATTERN, getPrefix(muleContext), configName);

    HttpClientConfiguration configuration = new HttpClientConfiguration.Builder()
        .setUriParameters(new DefaultUriParameters(protocol, host, port)).setAuthentication(authentication)
        .setTlsContextFactory(tlsContextFactory).setProxyConfig(proxyConfig).setClientSocketProperties(clientSocketProperties)
        .setMaxConnections(maxConnections).setUsePersistentConnections(usePersistentConnections)
        .setConnectionIdleTimeout(connectionIdleTimeout).setThreadNamePrefix(threadNamePrefix).setOwnerName(configName).build();

    HttpClient httpClient;
    if (httpClientFactory == null) {
      httpClient = new GrizzlyHttpClient(configuration);
    } else {
      httpClient = httpClientFactory.create(configuration);
    }

    return httpClient;
  }

  @Override
  public void disconnect(HttpClient httpClient) {}

  public Function<Event, Integer> getPort() {
    return port;
  }

  public ProxyConfig getProxyConfig() {
    return proxyConfig;
  }

  public TlsContextFactory getTlsContext() {
    return tlsContextFactory;
  }

  public HttpAuthentication getAuthentication() {
    return authentication;
  }
}
