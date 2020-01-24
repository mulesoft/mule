/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.store.ObjectStoreSettings.unmanagedTransient;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.getCallbackValuesExtractors;
import static org.mule.runtime.oauth.api.builder.ClientCredentialsLocation.BODY;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.extension.api.connectivity.oauth.PlatformManagedOAuthGrantType;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthConfig;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthHandler;
import org.mule.runtime.module.extension.internal.store.LazyObjectStoreToMapAdapter;
import org.mule.runtime.oauth.api.PlatformManagedOAuthDancer;
import org.mule.runtime.oauth.api.builder.OAuthPlatformManagedDancerBuilder;
import org.mule.runtime.oauth.api.listener.PlatformManagedOAuthStateListener;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.util.List;
import java.util.function.Function;

/**
 * A {@link OAuthHandler} implementation that works with a {@link PlatformManagedOAuthDancer}
 *
 * @since 4.3.0
 */
public class PlatformManagedOAuthHandler extends OAuthHandler<PlatformManagedOAuthDancer> {

  /**
   * Returns a {@link PlatformManagedOAuthDancer} configured after the given {@code config}
   *
   * @param config an {@link PlatformManagedOAuthConfig}
   * @return a {@link PlatformManagedOAuthDancer}
   */
  public PlatformManagedOAuthDancer register(PlatformManagedOAuthConfig config) {
    return register(config, emptyList());
  }

  /**
   * Returns a {@link PlatformManagedOAuthDancer} configured after the given {@code config}
   *
   * @param config an {@link PlatformManagedOAuthConfig}
   * @param listeners a list of {@link PlatformManagedOAuthStateListener listeners} to be registered into the dancer
   * @return a {@link PlatformManagedOAuthDancer}
   */
  public PlatformManagedOAuthDancer register(PlatformManagedOAuthConfig config,
                                             List<PlatformManagedOAuthStateListener> listeners) {
    return dancers.computeIfAbsent(config.getOwnerConfigName(),
                                   (CheckedFunction<String, PlatformManagedOAuthDancer>) k -> createDancer(config, listeners));

  }

  /**
   * Performs the refresh token flow
   *
   * @param config a registered {@link PlatformManagedOAuthConfig}
   */
  public void refreshToken(PlatformManagedOAuthConfig config) {
    PlatformManagedOAuthDancer dancer = dancers.get(config.getOwnerConfigName());

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
   * @param config a {@link PlatformManagedOAuthConfig}
   * @return the {@link ResourceOwnerOAuthContext} for the given {@code config}.
   */
  public ResourceOwnerOAuthContext getOAuthContext(PlatformManagedOAuthConfig config) {
    PlatformManagedOAuthDancer dancer = dancers.get(config.getOwnerConfigName());
    if (dancer == null) {
      throw new IllegalStateException(format("PlatformManaged dancer for config '%s' not yet registered",
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
   * @param config a registered {@link PlatformManagedOAuthConfig}
   */
  public void invalidate(PlatformManagedOAuthConfig config) {
    PlatformManagedOAuthDancer dancer = dancers.get(config.getOwnerConfigName());
    if (dancer == null) {
      return;
    }

    dancer.invalidateContext();
  }

  private PlatformManagedOAuthDancer createDancer(PlatformManagedOAuthConfig config,
                                                  List<PlatformManagedOAuthStateListener> listeners)
      throws MuleException {
    checkArgument(listeners != null, "listeners cannot be null");

    OAuthPlatformManagedDancerBuilder dancerBuilder =
        oauthService.get().platformManagedOAuthDancerBuilder(lockFactory,
                                                             new LazyObjectStoreToMapAdapter(
                                                                                             () -> objectStoreLocator
                                                                                                 .apply(config)),
                                                             expressionEvaluator);

    final PlatformManagedOAuthGrantType grantType = config.getGrantType();

    dancerBuilder
        .connectionUri(config.getConnectionUri())
        .platformUrl(config.getServiceUrl())
        .organizationId(config.getOrgId())
        .environmentId(config.getEnvironmentId())
        .name(config.getOwnerConfigName())
        .tokenUrl(config.getPlatformAuthUrl())
        .encoding(config.getEncoding())
        .clientCredentials(config.getClientId(), config.getClientSecret())
        .tokenUrl(config.getPlatformAuthUrl())
        .responseExpiresInExpr(config.getDelegateGrantType().getExpirationRegex())
        .responseAccessTokenExpr(grantType.getAccessTokenExpr())
        .withClientCredentialsIn(BODY)
        .resourceOwnerIdTransformer(ownerId -> ownerId + "-" + config.getOwnerConfigName());

    dancerBuilder.customParametersExtractorsExprs(
                                                  getParameterExtractors(getCallbackValuesExtractors(config
                                                      .getDelegateConnectionProviderModel())));

    listeners.forEach(dancerBuilder::addListener);

    PlatformManagedOAuthDancer dancer = dancerBuilder.build();

    if (started) {
      start(dancer);
    }

    return dancer;
  }

  @Override
  protected Function<OAuthConfig, ObjectStore> buildObjectStoreLocator() {
    return config -> objectStoreManager.getOrCreateObjectStore(config.getOwnerConfigName() + "-OCS-tokenStore",
                                                               unmanagedTransient());
  }
}
