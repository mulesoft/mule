/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.clientcredentials;

import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.oauth.api.ClientCredentialsConfig.builder;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.mule.service.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.oauth2.internal.AbstractGrantType;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.util.store.ObjectStoreToMapAdapter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.oauth.api.OAuthDancer;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.service.http.api.domain.message.request.HttpRequestBuilder;

import org.slf4j.Logger;

/**
 * Authorization element for client credentials oauth grant type
 */
public class ClientCredentialsGrantType extends AbstractGrantType implements Lifecycle {

  private static final Logger LOGGER = getLogger(ClientCredentialsGrantType.class);

  /**
   * Scope required by this application to execute. Scopes define permissions over resources.
   */
  @Parameter
  @Optional
  private String scopes;

  /**
   * If true, the client id and client secret will be sent in the request body. Otherwise, they will be sent as basic
   * authentication.
   */
  @Parameter
  @Optional(defaultValue = "false")
  private boolean encodeClientCredentialsInBody;

  private OAuthDancer dancer;

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();

    try {
      OAuthService oauthService = muleContext.getRegistry().lookupObject(OAuthService.class);

      dancer =
          oauthService.createClientCredentialsGrantTypeDancer(builder(getClientId(), getClientSecret(), getTokenUrl())
              .encodeClientCredentialsInBody(encodeClientCredentialsInBody)
              .scopes(scopes)
              .encoding(getDefaultEncoding(muleContext))
              .tlsContextFactory(getTlsContextFactory())
              .responseAccessTokenExpr(resolver.getExpression(getResponseAccessToken()))
              .responseRefreshTokenExpr(resolver.getExpression(getResponseRefreshToken()))
              .responseExpiresInExpr(resolver.getExpression(getResponseExpiresIn()))
              .customParametersExtractorsExprs(getCustomParameterExtractors().stream()
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

  @Override
  public void authenticate(HttpRequestBuilder builder) throws MuleException {
    builder.addHeader(AUTHORIZATION, buildAuthorizationHeaderContent(dancer.accessToken(DEFAULT_RESOURCE_OWNER_ID)));
  }

  @Override
  public boolean shouldRetry(final Result<Object, HttpResponseAttributes> firstAttemptResult) {
    final Boolean shouldRetryRequest = resolver.resolveExpression(getRefreshTokenWhen(), firstAttemptResult);
    if (shouldRetryRequest) {
      dancer.refreshToken(DEFAULT_RESOURCE_OWNER_ID, null);
    }
    return shouldRetryRequest;
  }
}
