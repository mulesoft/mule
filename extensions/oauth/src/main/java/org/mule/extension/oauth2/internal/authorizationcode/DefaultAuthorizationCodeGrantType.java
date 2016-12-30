/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.authorizationcode;

import static java.lang.String.format;
import static org.mule.extension.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.mule.extension.http.internal.HttpConnectorConstants.TLS_CONFIGURATION;
import static org.mule.extension.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.SECURITY_TAB;

import org.mule.extension.http.internal.listener.server.HttpListenerConfig;
import org.mule.extension.oauth2.api.RequestAuthenticationException;
import org.mule.extension.oauth2.internal.AbstractGrantType;
import org.mule.extension.oauth2.internal.authorizationcode.state.ConfigOAuthContext;
import org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.HttpServerConfiguration;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the config element for oauth:authentication-code-config.
 * <p/>
 * This config will: - If the authorization-request is defined then it will create a flow listening for an user call to begin the
 * oauth login. - If the token-request is defined then it will create a flow for listening in the redirect uri so we can get the
 * authentication code and retrieve the access token
 */
@Alias("authorization-code-grant-type")
public class DefaultAuthorizationCodeGrantType extends AbstractGrantType implements Lifecycle, AuthorizationCodeGrantType {

  private static final Logger logger = LoggerFactory.getLogger(DefaultAuthorizationCodeGrantType.class);

  /**
   * Application identifier as defined in the oauth authentication server.
   */
  @Parameter
  private String clientId;

  /**
   * Application secret as defined in the oauth authentication server.
   */
  @Parameter
  private String clientSecret;

  /**
   * Listener configuration to be used instead of localCallbackUrl. Note that if using this you must also provide a
   * localCallbackConfigPath separately.
   */
  @UseConfig
  @Optional
  private HttpListenerConfig localCallbackConfig;

  /**
   * Local path for the listener that will be created according to localCallbackConfig, not required if using localCallbackUrl.
   */
  @Parameter
  @Optional
  private String localCallbackConfigPath;

  /**
   * If this attribute is provided mule will automatically create an endpoint in this url to be able to store the authentication
   * code unless there's already an endpoint registered to manually extract the authorization code.
   */
  @Parameter
  @Optional
  private String localCallbackUrl;

  /**
   * The oauth authentication server will use this url to provide the authentication code to the Mule server so the mule server
   * can retrieve the access token.
   * <p>
   * Note that this must be the externally visible address of the callback, not the local one.
   */
  @Parameter
  private String externalCallbackUrl;

  /**
   * This element configures an automatic flow created by mule to handle
   */
  @Parameter
  @ParameterGroup("authorization-request")
  private AuthorizationRequestHandler authorizationRequestHandler;

  /**
   * This element configures an automatic flow created by mule that listens in the configured url by the redirectUrl attribute and
   * process the request to retrieve an access token from the oauth authentication server.
   */
  @Parameter
  @ParameterGroup("token-request")
  private AutoAuthorizationCodeTokenRequestHandler tokenRequestHandler;

  private HttpService httpService;
  private SchedulerService schedulerService;

  /**
   * References a TLS config that will be used to receive incoming HTTP request and do HTTP request during the OAuth dance.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  @DisplayName(TLS_CONFIGURATION)
  @Placement(tab = SECURITY_TAB)
  private TlsContextFactory tlsContextFactory;

  private HttpServer server;

  /**
   * Identifier under which the oauth authentication attributes are stored (accessToken, refreshToken, etc).
   * <p>
   * This attribute is only required when the applications needs to access resources from more than one user in the OAuth
   * authentication server.
   */
  @Parameter
  @Optional(defaultValue = DEFAULT_RESOURCE_OWNER_ID)
  private Function<Event, String> resourceOwnerId;

  @Override
  public HttpListenerConfig getLocalCallbackConfig() {
    return localCallbackConfig;
  }

  @Override
  public String getLocalCallbackConfigPath() {
    return localCallbackConfigPath;
  }

  @Override
  public String getLocalCallbackUrl() {
    return localCallbackUrl;
  }

  @Override
  public String getExternalCallbackUrl() {
    return externalCallbackUrl;
  }

  @Override
  public ConfigOAuthContext getUserOAuthContext() {
    return tokenManager.getConfigOAuthContext();
  }

  @Override
  public String getClientSecret() {
    return clientSecret;
  }

  @Override
  public String getClientId() {
    return clientId;
  }

  @Override
  public TlsContextFactory getTlsContext() {
    return tlsContextFactory;
  }

  @Override
  public void initialise() throws InitialisationException {
    try {
      this.httpService = muleContext.getRegistry().lookupObject(HttpService.class);
      this.schedulerService = muleContext.getSchedulerService();

      if (tokenManager == null) {
        this.tokenManager = TokenManagerConfig.createDefault(muleContext);
      }
      initialiseIfNeeded(tokenManager, muleContext);

      if (localCallbackConfig != null && localCallbackUrl != null) {
        throw new IllegalArgumentException("Attributes localCallbackConfig and localCallbackUrl are mutually exclusive");
      }
      if ((localCallbackConfig == null) != (localCallbackConfigPath == null)) {
        throw new IllegalArgumentException("Attributes localCallbackConfig and localCallbackConfigPath must be both present or absent");
      }

      if (tlsContextFactory != null) {
        initialiseIfNeeded(tlsContextFactory);
        tokenRequestHandler.setTlsContextFactory(tlsContextFactory);
      }
      tokenRequestHandler.setMuleContext(muleContext);
      tokenRequestHandler.initialise();

      buildHttpServer();
    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }
  }

  private void buildHttpServer() throws InitialisationException {
    final HttpServerConfiguration.Builder serverConfigBuilder = new HttpServerConfiguration.Builder();

    if (getLocalCallbackUrl() != null) {
      try {
        final URL localCallbackUrl = new URL(getLocalCallbackUrl());
        serverConfigBuilder.setHost(localCallbackUrl.getHost()).setPort(localCallbackUrl.getPort());
      } catch (MalformedURLException e) {
        logger.warn("Could not parse provided url %s. Validate that the url is correct", getLocalCallbackUrl());
        throw new InitialisationException(e, this);
      }
      // TODO MULE-11276 - Need a way to reuse an http listener declared in the application/domain")
      // } else if (getLocalCallbackConfig() != null) {
      // serverConfigBuilder
      // .setHost(getLocalCallbackConfig().getHost())
      // .setPort(getLocalCallbackConfig().getPort())
      // .setTlsContextFactory(getLocalCallbackConfig().getTlsContext());
    } else {
      throw new IllegalStateException("No localCallbackUrl or localCallbackConfig defined.");
    }

    if (getTlsContext() != null) {
      serverConfigBuilder.setTlsContextFactory(getTlsContext());
    }

    // TODO MULE-11272 Change to cpu-lite
    HttpServerConfiguration serverConfiguration =
        serverConfigBuilder.setSchedulerSupplier(() -> schedulerService.ioScheduler()).build();

    try {
      server = httpService.getServerFactory().create(serverConfiguration);
    } catch (ConnectionException e) {
      logger.warn("Could not create server for OAuth callback.");
      throw new InitialisationException(e, this);
    }

  }

  @Override
  public void authenticate(Event muleEvent, HttpRequestBuilder builder) throws MuleException {
    final String accessToken =
        getUserOAuthContext().getContextForResourceOwner(resourceOwnerId.apply(muleEvent)).getAccessToken();
    if (accessToken == null) {
      throw new RequestAuthenticationException(createStaticMessage(format("No access token for the %s user. Verify that you have authenticated the user before trying to execute an operation to the API.",
                                                                          resourceOwnerId)));
    }
    builder.addHeader(AUTHORIZATION, buildAuthorizationHeaderContent(accessToken));
  }

  @Override
  public boolean shouldRetry(final Event firstAttemptResponseEvent) throws MuleException {
    Boolean shouldRetryRequest = tokenRequestHandler.getRefreshTokenWhen().apply(firstAttemptResponseEvent);
    if (shouldRetryRequest) {
      try {
        tokenRequestHandler.refreshToken(firstAttemptResponseEvent, resourceOwnerId.apply(firstAttemptResponseEvent));
      } catch (MuleException e) {
        throw new MuleRuntimeException(e);
      }
    }
    return shouldRetryRequest;
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    super.setMuleContext(muleContext);
    authorizationRequestHandler.setMuleContext(muleContext);
  }

  @Override
  public void start() throws MuleException {
    try {
      server.start();
    } catch (IOException e) {
      throw new DefaultMuleException(e);
    }

    if (authorizationRequestHandler != null) {
      authorizationRequestHandler.setOauthConfig(this);
      authorizationRequestHandler.init();
      authorizationRequestHandler.start();
    }
    if (tokenRequestHandler != null) {
      tokenRequestHandler.setOauthConfig(this);
      tokenRequestHandler.init();
      tokenRequestHandler.start();
    }
  }

  @Override
  public void stop() throws MuleException {
    tokenRequestHandler.stop();
    authorizationRequestHandler.stop();
    server.stop();
  }

  @Override
  public void dispose() {
    server.dispose();
  }

  @Override
  public HttpServer getServer() {
    return server;
  }
}
