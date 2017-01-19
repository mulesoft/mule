/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.clientcredentials;

import static java.util.stream.Collectors.toMap;
import static org.mule.extension.http.internal.HttpConnectorConstants.TLS_CONFIGURATION;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.SECURITY_TAB;
import static org.mule.runtime.oauth.api.ClientCredentialsConfig.builder;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.mule.service.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.oauth2.internal.AbstractGrantType;
import org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.util.store.ObjectStoreToMapAdapter;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.oauth.api.OAuthDancer;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.service.http.api.domain.message.request.HttpRequestBuilder;

import org.slf4j.Logger;

/**
 * Authorization element for client credentials oauth grant type
 */
public class ClientCredentialsGrantType extends AbstractGrantType implements Initialisable, Startable, Stoppable, Disposable {

  private static final Logger LOGGER = getLogger(ClientCredentialsGrantType.class);

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
   * This element configures an automatic flow created by mule that listens in the configured url by the redirectUrl attribute and
   * process the request to retrieve an access token from the oauth authentication server.
   */
  @Parameter
  @ParameterGroup(name = "Token Request")
  private ClientCredentialsTokenRequestHandler tokenRequestHandler;

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

  public TlsContextFactory getTlsContext() {
    return tlsContextFactory;
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
    LifecycleUtils.disposeIfNeeded(dancer, LOGGER);
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
  public void initialise() throws InitialisationException {
    if (tokenManager == null) {
      this.tokenManager = TokenManagerConfig.createDefault(muleContext);
    }
    initialiseIfNeeded(tokenManager, muleContext);

    if (tlsContextFactory != null) {
      initialiseIfNeeded(tlsContextFactory);
    }

    try {
      OAuthService oauthService = muleContext.getRegistry().lookupObject(OAuthService.class);

      dancer =
          oauthService.createClientCredentialsGrantTypeDancer(builder(clientId, clientSecret, tokenRequestHandler.getTokenUrl())
              .encodeClientCredentialsInBody(tokenRequestHandler.isEncodeClientCredentialsInBody())
              .scopes(tokenRequestHandler.getScopes())
              .encoding(getDefaultEncoding(muleContext))
              .tlsContextFactory(tlsContextFactory)
              .responseAccessTokenExpr(resolver.getExpression(tokenRequestHandler.getResponseAccessToken()))
              .responseRefreshTokenExpr(resolver.getExpression(tokenRequestHandler.getResponseRefreshToken()))
              .responseExpiresInExpr(resolver.getExpression(tokenRequestHandler.getResponseExpiresIn()))
              .customParametersExtractorsExprs(tokenRequestHandler.getCustomParameterExtractors().stream()
                  .collect(toMap(extractor -> extractor.getParamName(),
                                 extractor -> resolver.getExpression(extractor.getValue()))))
              .build(),
                                                              lockId -> muleContext.getLockFactory().createLock(lockId),
                                                              new ObjectStoreToMapAdapter(tokenManager.getObjectStore()),
                                                              muleContext.getExpressionManager());
    } catch (RegistrationException e) {
      throw new InitialisationException(e, this);
    }
    initialiseIfNeeded(dancer);
  }

  @Override
  public void authenticate(HttpRequestBuilder builder) throws MuleException {
    builder.addHeader(AUTHORIZATION, buildAuthorizationHeaderContent(dancer.accessToken(DEFAULT_RESOURCE_OWNER_ID)));
  }

  @Override
  public boolean shouldRetry(final Result<Object, HttpResponseAttributes> firstAttemptResult) {
    final Boolean shouldRetryRequest =
        resolver.resolveExpression(tokenRequestHandler.getRefreshTokenWhen(), firstAttemptResult);
    if (shouldRetryRequest) {
      dancer.refreshToken(DEFAULT_RESOURCE_OWNER_ID, null);
    }
    return shouldRetryRequest;
  }

  public void setTokenManager(TokenManagerConfig tokenManager) {
    this.tokenManager = tokenManager;
  }
}
