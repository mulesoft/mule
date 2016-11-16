/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import static java.lang.String.format;
import static org.mule.extension.http.internal.HttpConnectorConstants.AUTHENTICATION;
import static org.mule.extension.http.internal.HttpConnectorConstants.TLS;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.util.concurrent.ThreadNameHelper.getPrefix;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTPS;
import org.mule.extension.http.api.request.authentication.HttpAuthentication;
import org.mule.extension.http.api.request.client.HttpClient;
import org.mule.extension.http.api.request.proxy.ProxyConfig;
import org.mule.extension.http.internal.listener.HttpTlsParams;
import org.mule.extension.http.internal.request.client.DefaultUriParameters;
import org.mule.extension.http.internal.request.client.HttpClientConfiguration;
import org.mule.extension.http.internal.request.client.HttpClientFactory;
import org.mule.extension.http.internal.request.grizzly.GrizzlyHttpClient;
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
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
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

  @ParameterGroup(name = CONNECTION)
  private RequestConnectionParams connectionParams;

  @ParameterGroup(name = TLS)
  private HttpTlsParams tlsParams;

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

  private HttpClientFactory httpClientFactory;

  @Override
  public ConnectionValidationResult validate(HttpClient httpClient) {
    return ConnectionValidationResult.success();
  }

  @Override
  public void initialise() throws InitialisationException {
    final HttpConstants.Protocols protocol = connectionParams.getProtocol();
    TlsContextFactory tlsContextFactory = tlsParams.getTlsContext();

    if (connectionParams.getPort() == null) {
      connectionParams.setPort(muleEvent -> protocol.getDefaultPort());
    }

    if (protocol.equals(HTTP) && tlsContextFactory != null) {
      throw new InitialisationException(createStaticMessage("TlsContext cannot be configured with protocol HTTP, "
          + "when using tls:context you must set attribute protocol=\"HTTPS\""),
                                        this);
    }

    if (protocol.equals(HTTPS) && tlsContextFactory == null) {
      // MULE-9480
      initialiseIfNeeded(defaultTlsContextFactoryBuilder);
      tlsContextFactory = defaultTlsContextFactoryBuilder.buildDefault();
      tlsParams.setTlsContext(tlsContextFactory);
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
  public HttpClient connect() throws ConnectionException {
    String threadNamePrefix = format(THREAD_NAME_PREFIX_PATTERN, getPrefix(muleContext), configName);

    HttpClientConfiguration configuration = new HttpClientConfiguration.Builder()
        .setUriParameters(new DefaultUriParameters(connectionParams.getProtocol(), connectionParams.getHost(),
                                                   connectionParams.getPort()))
        .setAuthentication(authentication)
        .setTlsContextFactory(tlsParams.getTlsContext())
        .setProxyConfig(proxyConfig)
        .setClientSocketProperties(connectionParams.getClientSocketProperties())
        .setMaxConnections(connectionParams.getMaxConnections())
        .setUsePersistentConnections(connectionParams.getUsePersistentConnections())
        .setConnectionIdleTimeout(connectionParams.getConnectionIdleTimeout())
        .setThreadNamePrefix(threadNamePrefix)
        .setOwnerName(configName)
        .build();

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
    return connectionParams.getPort();
  }

  public ProxyConfig getProxyConfig() {
    return proxyConfig;
  }

  public TlsContextFactory getTlsContext() {
    return tlsParams.getTlsContext();
  }

  public HttpAuthentication getAuthentication() {
    return authentication;
  }
}
