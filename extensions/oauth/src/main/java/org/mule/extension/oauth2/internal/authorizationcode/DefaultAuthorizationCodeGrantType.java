/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.authorizationcode;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.mule.extension.http.internal.HttpConnectorConstants.TLS_CONFIGURATION;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.SECURITY_TAB;
import static org.mule.runtime.oauth.api.AuthorizationCodeOAuthConfig.builder;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.mule.service.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.http.internal.listener.server.HttpListenerConfig;
import org.mule.extension.oauth2.internal.AbstractGrantType;
import org.mule.extension.oauth2.internal.TokenRequestHandler;
import org.mule.extension.oauth2.internal.authorizationcode.state.ConfigOAuthContext;
import org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.util.store.ObjectStoreToMapAdapter;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.runtime.operation.ParameterResolver;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.oauth.api.AuthorizationCodeOAuthConfig.AuthorizationCodeOAuthConfigBuilder;
import org.mule.runtime.oauth.api.OAuthDancer;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.service.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.service.http.api.server.HttpServer;

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;

/**
 * Represents the config element for {@code oauth:authentication-code-config}.
 * <p>
 * This config will: - If the authorization-request is defined then it will create a flow listening for an user call to begin the
 * oauth login. - If the token-request is defined then it will create a flow for listening in the redirect uri so we can get the
 * authentication code and retrieve the access token
 */
@Alias("authorization-code-grant-type")
public class DefaultAuthorizationCodeGrantType extends AbstractGrantType implements Lifecycle, AuthorizationCodeGrantType {

  private static final Logger LOGGER = getLogger(DefaultAuthorizationCodeGrantType.class);

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
  @ParameterGroup(name = "authorization-request")
  private AuthorizationRequestHandler authorizationRequestHandler;

  /**
   * This element configures an automatic flow created by mule that listens in the configured url by the redirectUrl attribute and
   * process the request to retrieve an access token from the oauth authentication server.
   */
  @Parameter
  @ParameterGroup(name = "token-request")
  private TokenRequestHandler tokenRequestHandler;

  /**
   * References a TLS config that will be used to receive incoming HTTP request and do HTTP request during the OAuth dance.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  @DisplayName(TLS_CONFIGURATION)
  @Placement(tab = SECURITY_TAB)
  private TlsContextFactory tlsContextFactory;

  private OAuthDancer dancer;

  /**
   * Identifier under which the oauth authentication attributes are stored (accessToken, refreshToken, etc).
   * <p>
   * This attribute is only required when the applications needs to access resources from more than one user in the OAuth
   * authentication server.
   */
  @Parameter
  @Optional(defaultValue = DEFAULT_RESOURCE_OWNER_ID)
  private ParameterResolver<String> resourceOwnerId;

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
      }
    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }

    try {
      OAuthService oauthService = muleContext.getRegistry().lookupObject(OAuthService.class);

      AuthorizationCodeOAuthConfigBuilder configBuilder = builder(clientId, clientSecret, tokenRequestHandler.getTokenUrl())
          .externalCallbackUrl(externalCallbackUrl);

      if (localCallbackUrl != null) {
        configBuilder.localCallback(new URL(localCallbackUrl), ofNullable(tlsContextFactory));
      } else if (localCallbackConfig != null) {
        // TODO MULE-11276 - Need a way to reuse an http listener declared in the application/domain")
        HttpServer server = null;
        configBuilder.localCallback(server, localCallbackConfigPath);
        throw new UnsupportedOperationException("Not implemented yet.");
      }

      dancer =
          oauthService
              .createAuthorizationCodeGrantTypeDancer(configBuilder
                  .localAuthorizationUrlPath(new URL(authorizationRequestHandler.getLocalAuthorizationUrl()).getPath())
                  .localAuthorizationUrlResourceOwnerId(resolver
                      .getExpression(authorizationRequestHandler.getLocalAuthorizationUrlResourceOwnerId()))
                  .customParameters(authorizationRequestHandler.getCustomParameters())
                  .state(resolver.getExpression(authorizationRequestHandler.getState()))
                  .authorizationUrl(authorizationRequestHandler.getAuthorizationUrl())
                  .encoding(getDefaultEncoding(muleContext))
                  .tlsContextFactory(tlsContextFactory)
                  .responseAccessTokenExpr(resolver.getExpression(tokenRequestHandler.getResponseAccessToken()))
                  .responseRefreshTokenExpr(resolver.getExpression(tokenRequestHandler.getResponseRefreshToken()))
                  .responseExpiresInExpr(resolver.getExpression(tokenRequestHandler.getResponseExpiresIn()))
                  .customParametersExtractorsExprs(tokenRequestHandler.getCustomParameterExtractors().stream()
                      .collect(toMap(extractor -> extractor.getParamName(),
                                     extractor -> resolver.getExpression(extractor.getValue()))))
                  .scopes(authorizationRequestHandler.getScopes())
                  .build(),
                                                      lockId -> muleContext.getLockFactory().createLock(lockId),
                                                      new ObjectStoreToMapAdapter(tokenManager.getObjectStore()),
                                                      muleContext.getExpressionManager());
    } catch (RegistrationException | MalformedURLException e) {
      throw new InitialisationException(e, this);
    }
    initialiseIfNeeded(dancer);
  }

  @Override
  public void authenticate(HttpRequestBuilder builder) throws MuleException {
    builder.addHeader(AUTHORIZATION, buildAuthorizationHeaderContent(dancer.accessToken(resourceOwnerId.resolve())));
  }

  @Override
  public boolean shouldRetry(final Result<Object, HttpResponseAttributes> firstAttemptResult) throws MuleException {
    Boolean shouldRetryRequest = resolver.resolveExpression(tokenRequestHandler.getRefreshTokenWhen(), firstAttemptResult);
    if (shouldRetryRequest) {
      dancer.refreshToken(resolver.resolveExpression(resourceOwnerId, firstAttemptResult), null);
    }
    return shouldRetryRequest;
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(dancer);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(dancer);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(dancer, LOGGER);
  }
}
