/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.clientcredentials;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.service.http.api.HttpHeaders.Names.AUTHORIZATION;

import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.oauth2.internal.AbstractGrantType;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.util.store.SimpleObjectStoreToMapAdapter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.oauth.api.ClientCredentialsOAuthDancer;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.runtime.oauth.api.builder.OAuthClientCredentialsDancerBuilder;
import org.mule.service.http.api.domain.message.request.HttpRequestBuilder;

import java.util.concurrent.ExecutionException;

/**
 * Authorization element for client credentials oauth grant type
 */
public class ClientCredentialsGrantType extends AbstractGrantType {

  /**
   * If true, the client id and client secret will be sent in the request body. Otherwise, they will be sent as basic
   * authentication.
   */
  @Parameter
  @Optional(defaultValue = "false")
  private boolean encodeClientCredentialsInBody;

  private ClientCredentialsOAuthDancer dancer;

  @Override
  public final void initialise() throws InitialisationException {
    initTokenManager();

    try {
      OAuthService oauthService = muleContext.getRegistry().lookupObject(OAuthService.class);

      OAuthClientCredentialsDancerBuilder dancerBuilder =
          oauthService.clientCredentialsGrantTypeDancerBuilder(lockId -> muleContext.getLockFactory().createLock(lockId),
                                                               new SimpleObjectStoreToMapAdapter(tokenManager.getObjectStore()),
                                                               muleContext.getExpressionManager());
      dancerBuilder.encodeClientCredentialsInBody(encodeClientCredentialsInBody);
      dancerBuilder.clientCredentials(getClientId(), getClientSecret());

      configureBaseDancer(dancerBuilder);
      dancer = dancerBuilder.build();
    } catch (RegistrationException e) {
      throw new InitialisationException(e, this);
    }
    initialiseIfNeeded(getDancer());
  }

  @Override
  public void authenticate(HttpRequestBuilder builder) throws MuleException {
    try {
      builder.addHeader(AUTHORIZATION, buildAuthorizationHeaderContent(dancer.accessToken().get()));
    } catch (InterruptedException e) {
      currentThread().interrupt();
      throw new DefaultMuleException(e);
    } catch (ExecutionException e) {
      throw new DefaultMuleException(e.getCause());
    }
  }

  @Override
  public boolean shouldRetry(final Result<Object, HttpResponseAttributes> firstAttemptResult) throws MuleException {
    final Boolean shouldRetryRequest = resolver.resolveExpression(getRefreshTokenWhen(), firstAttemptResult);
    if (shouldRetryRequest) {
      try {
        dancer.refreshToken().get();
      } catch (InterruptedException e) {
        currentThread().interrupt();
        throw new DefaultMuleException(e);
      } catch (ExecutionException e) {
        throw new DefaultMuleException(e.getCause());
      }
    }
    return shouldRetryRequest;
  }

  @Override
  public ClientCredentialsOAuthDancer getDancer() {
    return dancer;
  }
}
