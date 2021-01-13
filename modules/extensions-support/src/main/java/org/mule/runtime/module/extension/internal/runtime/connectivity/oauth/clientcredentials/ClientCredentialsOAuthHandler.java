/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.toCredentialsLocation;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthHandler;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.AuthorizationCodeConfig;
import org.mule.runtime.module.extension.internal.store.LazyObjectStoreToMapAdapter;
import org.mule.runtime.oauth.api.ClientCredentialsOAuthDancer;
import org.mule.runtime.oauth.api.listener.ClientCredentialsListener;
import org.mule.runtime.oauth.api.builder.OAuthClientCredentialsDancerBuilder;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.util.List;
import java.util.Objects;

/**
 * {@link OAuthHandler} implementation for the client credentials grant type
 *
 * @since 4.2.1
 */
public class ClientCredentialsOAuthHandler extends OAuthHandler<ClientCredentialsOAuthDancer> {

  /**
   * Becomes aware of the given {@code config} and makes sure that the access token callback
   * and authorization endpoints are provisioned.
   *
   * @param config an {@link ClientCredentialsConfig}
   */
  public ClientCredentialsOAuthDancer register(ClientCredentialsConfig config) {
    return register(config, emptyList());
  }

  public ClientCredentialsOAuthDancer register(ClientCredentialsConfig config, List<ClientCredentialsListener> listeners) {
    return dancers.computeIfAbsent(config.getConfigIdentifier(),
                                   (CheckedFunction<String, ClientCredentialsOAuthDancer>) k -> createDancer(config, listeners));
  }

  /**
   * Performs the refresh token flow
   *
   * @param config a registered {@link ClientCredentialsConfig}
   */
  public void refreshToken(ClientCredentialsConfig config) {
    ClientCredentialsOAuthDancer dancer = dancers.get(config.getConfigIdentifier());

    try {
      dancer.refreshToken().get();
    } catch (Exception e) {
      throw new MuleRuntimeException(
                                     createStaticMessage(format("Could not refresh token for config '%s'",
                                                                config.getOwnerConfigName())),
                                     e);
    }
  }

  /**
   * Retrieves the {@link ResourceOwnerOAuthContext} for the given {@code config}. If no such context yet exists,
   * then it performs the OAuth authorization and returns the resulting context.
   *
   * @param config a {@link ClientCredentialsConfig}
   * @return the {@link ResourceOwnerOAuthContext} for the given {@code config}.
   */
  public ResourceOwnerOAuthContext getOAuthContext(ClientCredentialsConfig config) {
    ClientCredentialsOAuthDancer dancer = dancers.get(config.getConfigIdentifier());
    if (dancer == null) {
      throw new IllegalStateException(
                                      format("Client Credentials dancer for config '%s' not yet registered",
                                             config.getOwnerConfigName()));
    }

    ResourceOwnerOAuthContext contextForResourceOwner = dancer.getContext();

    if (contextForResourceOwner == null || contextForResourceOwner.getAccessToken() == null) {
      try {
        dancer.accessToken().get();
        contextForResourceOwner = dancer.getContext();
      } catch (Exception e) {
        throw new MuleRuntimeException(
                                       createStaticMessage(format("Could not obtain access token for config '%s'",
                                                                  config.getOwnerConfigName())),
                                       e);
      }
    }

    return contextForResourceOwner;
  }

  /**
   * Invalidates the OAuth state associated to the given {@code config}
   *
   * @param config a registered {@link ClientCredentialsConfig}
   */
  public void invalidate(ClientCredentialsConfig config) {
    ClientCredentialsOAuthDancer dancer = dancers.get(config.getConfigIdentifier());
    if (dancer == null) {
      return;
    }

    dancer.invalidateContext();
  }

  private ClientCredentialsOAuthDancer createDancer(ClientCredentialsConfig config, List<ClientCredentialsListener> listeners)
      throws MuleException {
    checkArgument(listeners != null, "listeners cannot be null");

    OAuthClientCredentialsDancerBuilder dancerBuilder =
        oauthService.get().clientCredentialsGrantTypeDancerBuilder(lockFactory,
                                                                   new LazyObjectStoreToMapAdapter(
                                                                                                   () -> objectStoreLocator
                                                                                                       .apply(config)),
                                                                   expressionEvaluator);

    final ClientCredentialsGrantType grantType = config.getGrantType();

    dancerBuilder
        .name(config.getOwnerConfigName())
        .encoding(getDefaultEncoding(muleContext))
        .clientCredentials(config.getClientId(), config.getClientSecret())
        .tokenUrl(config.getTokenUrl())
        .responseExpiresInExpr(grantType.getExpirationRegex())
        .responseAccessTokenExpr(grantType.getAccessTokenExpr())
        .withClientCredentialsIn(toCredentialsLocation(grantType.getCredentialsPlacement()))
        .resourceOwnerIdTransformer(ownerId -> ownerId + "-" + config.getOwnerConfigName() + "-" + generateId(config));

    String scopes = config.getScope()
        .orElseGet(() -> grantType.getDefaultScopes().orElse(null));

    if (scopes != null) {
      dancerBuilder.scopes(scopes);
    }

    dancerBuilder
        .customParameters(config.getCustomParameters())
        .customHeaders(config.getCustomHeaders())
        .customParametersExtractorsExprs(getParameterExtractors(config));

    listeners.forEach(dancerBuilder::addListener);

    ClientCredentialsOAuthDancer dancer = dancerBuilder.build();

    if (started) {
      start(dancer);
    }

    return dancer;
  }

  private Integer generateId(ClientCredentialsConfig config) {
    return Objects.hash(config.getOwnerConfigName(), config.getClientId(), config.getClientSecret(), config.getTokenUrl(),
                        config.getScope(), config.getCustomParameters(), config.getCustomHeaders());
  }
}
