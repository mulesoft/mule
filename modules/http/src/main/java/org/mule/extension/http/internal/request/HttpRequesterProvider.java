/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import static java.lang.String.format;
import static org.mule.extension.http.internal.HttpConnectorConstants.AUTHENTICATION;
import static org.mule.extension.http.internal.HttpConnectorConstants.TLS_CONFIGURATION;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.util.concurrent.ThreadNameHelper.getPrefix;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.SECURITY_TAB;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTPS;
import org.mule.extension.http.api.request.authentication.HttpAuthentication;
import org.mule.extension.http.api.request.client.UriParameters;
import org.mule.extension.http.internal.request.client.DefaultUriParameters;
import org.mule.extension.http.internal.request.client.HttpExtensionClient;
import org.mule.extension.socket.api.socket.tcp.TcpClientSocketProperties;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.tls.TlsContextFactoryBuilder;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.module.http.api.HttpConstants;
import org.mule.runtime.module.tls.api.DefaultTlsContextFactoryBuilder;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.client.HttpClientConfiguration;
import org.mule.service.http.api.client.proxy.ProxyConfig;

import java.util.function.Function;

import javax.inject.Inject;

/**
 * Connection provider for a HTTP request, handles the creation of {@link HttpExtensionClient} instances.
 *
 * @since 4.0
 */
@Alias("request")
public class HttpRequesterProvider implements CachedConnectionProvider<HttpExtensionClient>, Initialisable {

  private static final int UNLIMITED_CONNECTIONS = -1;
  private static final String THREAD_NAME_PREFIX_PATTERN = "%shttp.requester.%s";

  @Inject
  private MuleContext muleContext;

  @ConfigName
  private String configName;

  @ParameterGroup(CONNECTION)
  private RequestConnectionParams connectionParams;

  /**
   * Reference to a TLS config element. This will enable HTTPS for this config.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  @DisplayName(TLS_CONFIGURATION)
  @Placement(tab = SECURITY_TAB)
  private TlsContextFactory tlsContext;

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
   * Authentication method to use for the HTTP request.
   */
  @Parameter
  @Optional
  @Placement(tab = AUTHENTICATION)
  private HttpAuthentication authentication;

  @Inject
  @DefaultTlsContextFactoryBuilder
  private TlsContextFactoryBuilder defaultTlsContextFactoryBuilder;

  @Inject
  private HttpService httpService;

  @Override
  public ConnectionValidationResult validate(HttpExtensionClient httpClient) {
    return ConnectionValidationResult.success();
  }

  @Override
  public void initialise() throws InitialisationException {
    final HttpConstants.Protocols protocol = connectionParams.getProtocol();

    if (connectionParams.getPort() == null) {
      connectionParams.setPort(muleEvent -> protocol.getDefaultPort());
    }

    if (protocol.equals(HTTP) && tlsContext != null) {
      throw new InitialisationException(createStaticMessage("TlsContext cannot be configured with protocol HTTP, "
          + "when using tls:context you must set attribute protocol=\"HTTPS\""),
                                        this);
    }

    if (protocol.equals(HTTPS) && tlsContext == null) {
      // MULE-9480
      initialiseIfNeeded(defaultTlsContextFactoryBuilder);
      tlsContext = defaultTlsContextFactoryBuilder.buildDefault();
    }
    if (tlsContext != null) {
      initialiseIfNeeded(tlsContext);
    }
    if (authentication != null) {
      initialiseIfNeeded(authentication);
    }

    verifyConnectionsParameters();
  }

  private void verifyConnectionsParameters() throws InitialisationException {
    if (connectionParams.getMaxConnections() < UNLIMITED_CONNECTIONS || connectionParams.getMaxConnections() == 0) {
      throw new InitialisationException(createStaticMessage(
                                                            "The maxConnections parameter only allows positive values or -1 for unlimited concurrent connections."),
                                        this);
    }

    if (!connectionParams.getUsePersistentConnections()) {
      connectionParams.setConnectionIdleTimeout(0);
    }
  }

  @Override
  public HttpExtensionClient connect() throws ConnectionException {
    String threadNamePrefix = format(THREAD_NAME_PREFIX_PATTERN, getPrefix(muleContext), configName);

    HttpClientConfiguration configuration = new HttpClientConfiguration.Builder()
        .setTlsContextFactory(tlsContext)
        .setProxyConfig(proxyConfig)
        .setClientSocketProperties(new TcpClientSocketPropertiesAdapter(connectionParams.getClientSocketProperties()))
        .setMaxConnections(connectionParams.getMaxConnections())
        .setUsePersistentConnections(connectionParams.getUsePersistentConnections())
        .setConnectionIdleTimeout(connectionParams.getConnectionIdleTimeout())
        .setThreadNamePrefix(threadNamePrefix)
        .setOwnerName(configName)
        .build();

    HttpClient httpClient = httpService.getClientFactory().create(configuration);
    UriParameters uriParameters = new DefaultUriParameters(connectionParams.getProtocol(), connectionParams.getHost(),
                                                           connectionParams.getPort());
    return new HttpExtensionClient(httpClient, uriParameters, authentication);
  }

  @Override
  public void disconnect(HttpExtensionClient httpClient) {}

  public Function<Event, Integer> getPort() {
    return connectionParams.getPort();
  }

  public ProxyConfig getProxyConfig() {
    return proxyConfig;
  }

  public TlsContextFactory getTlsContext() {
    return tlsContext;
  }

  public HttpAuthentication getAuthentication() {
    return authentication;
  }


  private class TcpClientSocketPropertiesAdapter implements org.mule.service.http.api.tcp.TcpClientSocketProperties {

    TcpClientSocketProperties tcpClientSocketProperties;

    public TcpClientSocketPropertiesAdapter(TcpClientSocketProperties tcpClientSocketProperties) {
      this.tcpClientSocketProperties = tcpClientSocketProperties;
    }

    @Override
    public Integer getSendBufferSize() {
      return tcpClientSocketProperties.getSendBufferSize();
    }

    @Override
    public Integer getReceiveBufferSize() {
      return tcpClientSocketProperties.getReceiveBufferSize();
    }

    @Override
    public Boolean getSendTcpNoDelay() {
      return tcpClientSocketProperties.getSendTcpNoDelay();
    }

    @Override
    public Integer getConnectionTimeout() {
      return tcpClientSocketProperties.getConnectionTimeout();
    }

    @Override
    public Integer getLinger() {
      return tcpClientSocketProperties.getLinger();
    }

    @Override
    public Boolean getKeepAlive() {
      return tcpClientSocketProperties.getKeepAlive();
    }

    @Override
    public Integer getClientTimeout() {
      return tcpClientSocketProperties.getClientTimeout();
    }
  }

}
