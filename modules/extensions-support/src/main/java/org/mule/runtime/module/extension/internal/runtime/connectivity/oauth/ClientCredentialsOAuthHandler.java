/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.toCredentialsLocation;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.module.extension.internal.store.LazyObjectStoreToMapAdapter;
import org.mule.runtime.oauth.api.ClientCredentialsOAuthDancer;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.runtime.oauth.api.builder.ClientCredentialsListener;
import org.mule.runtime.oauth.api.builder.OAuthClientCredentialsDancerBuilder;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ClientCredentialsOAuthHandler extends BaseOAuthHandler<ClientCredentialsOAuthDancer> {

  public ClientCredentialsOAuthHandler(LazyValue<HttpService> httpService,
                                       LazyValue<OAuthService> oauthService,
                                       LockFactory lockFactory,
                                       MuleExpressionLanguage expressionEvaluator,
                                       Function<OAuthConfig, ObjectStore> objectStoreLocator,
                                       MuleContext muleContext) {
    super(httpService, oauthService, lockFactory, expressionEvaluator, objectStoreLocator, muleContext);
  }

  /**
   * Becomes aware of the given {@code config} and makes sure that the access token callback
   * and authorization endpoints are provisioned.
   *
   * @param config an {@link AuthorizationCodeConfig}
   */
  public ClientCredentialsOAuthDancer register(ClientCredentialsConfig config) {
    return register(config, emptyList());
  }

  public ClientCredentialsOAuthDancer register(ClientCredentialsConfig config, List<ClientCredentialsListener> listeners) {
    return dancers.computeIfAbsent(config.getOwnerConfigName(),
                                   (CheckedFunction<String, ClientCredentialsOAuthDancer>) k -> createDancer(config, listeners));

  }

  /**
   * Performs the refresh token flow
   *
   * @param ownerConfigName the name of the extension config which obtained the token
   */
  public void refreshToken(String ownerConfigName) {
    ClientCredentialsOAuthDancer dancer = dancers.get(ownerConfigName);

    try {
      dancer.refreshToken().get();
    } catch (Exception e) {
      throw new MuleRuntimeException(
          createStaticMessage(format("Could not refresh token for config '%s'", ownerConfigName)), e);
    }
  }

  /**
   * @param config an {@link OAuthConfig}
   * @return the {@link ResourceOwnerOAuthContext} for the given {@code config} or {@link Optional#empty()}
   * if authorization hasn't yet taken place or has been invalidated
   */
  public Optional<ResourceOwnerOAuthContext> getOAuthContext(ClientCredentialsConfig config) {
    ClientCredentialsOAuthDancer dancer = dancers.get(config.getOwnerConfigName());
    if (dancer == null) {
      return empty();
    }

    ResourceOwnerOAuthContext contextForResourceOwner = dancer.getContext();

    if (contextForResourceOwner == null || contextForResourceOwner.getAccessToken() == null) {
      return empty();
    }

    return of(contextForResourceOwner);
  }

  /**
   * Invalidates the OAuth state
   *
   * @param ownerConfigName the name of the extension config which obtained the token
   */
  public void invalidate(String ownerConfigName) {
    ClientCredentialsOAuthDancer dancer = dancers.get(ownerConfigName);
    if (dancer == null) {
      return;
    }

    dancer.invalidateContext();
  }

  private ClientCredentialsOAuthDancer createDancer(ClientCredentialsConfig config, List<ClientCredentialsListener> listeners)
      throws MuleException {
    checkArgument(listeners != null, "listeners cannot be null");

    OAuthClientCredentialsDancerBuilder dancerBuilder =
        oauthService.get().clientCredentialsGrantTypeDancerBuilder(lockId -> lockFactory.createLock(lockId),
                                                                   new LazyObjectStoreToMapAdapter(
                                                                       () -> objectStoreLocator.apply(config)),
                                                                   expressionEvaluator);

    final ClientCredentialsGrantType grantType = config.getGrantType();

    dancerBuilder
        .encoding(getDefaultEncoding(muleContext))
        .clientCredentials(config.getClientId(), config.getClientSecret())
        .tokenUrl(config.getTokenUrl())
        .responseExpiresInExpr(grantType.getExpirationRegex())
        .responseRefreshTokenExpr(grantType.getRefreshTokenExpr())
        .responseAccessTokenExpr(grantType.getAccessTokenExpr())
        .withClientCredentialsIn(toCredentialsLocation(grantType.getCredentialsPlacement()))
        .resourceOwnerIdTransformer(ownerId -> ownerId + "-" + config.getOwnerConfigName());

    String scopes = config.getScope()
        .orElseGet(() -> grantType.getDefaultScopes().orElse(null));

    if (scopes != null) {
      dancerBuilder.scopes(scopes);
    }


    dancerBuilder
        // TODO: Check with Rodro. It sounds like this should be supported
        //.customParameters(config.getCustomParameters())
        .customParametersExtractorsExprs(getParameterExtractors(config));

    listeners.forEach(dancerBuilder::addListener);

    ClientCredentialsOAuthDancer dancer = dancerBuilder.build();

    if (started) {
      start(dancer);
    }

    return dancer;
  }
}
