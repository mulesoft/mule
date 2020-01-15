/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode;

import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.ACCESS_TOKEN_URL_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.AFTER_FLOW_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.AUTHORIZATION_URL_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.BEFORE_FLOW_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.CALLBACK_PATH_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.CONSUMER_KEY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.CONSUMER_SECRET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.EXTERNAL_CALLBACK_URL_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.LISTENER_CONFIG_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.LOCAL_AUTHORIZE_PATH_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.OAUTH_AUTHORIZATION_CODE_GROUP_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.OAUTH_CALLBACK_GROUP_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.RESOURCE_OWNER_ID_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.SCOPES_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.getCallbackValuesExtractors;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.BaseOAuthConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthObjectStoreConfig;
import org.mule.runtime.module.extension.internal.runtime.resolver.MapValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

/**
 * A specialization of {@link BaseOAuthConnectionProviderObjectBuilder} to wrap the {@link ConnectionProvider}
 * into {@link AuthorizationCodeConnectionProviderWrapper} instances.
 *
 * @since 4.0
 */
public class AuthorizationCodeConnectionProviderObjectBuilder<C> extends BaseOAuthConnectionProviderObjectBuilder<C>
    implements Startable {

  private final AuthorizationCodeOAuthHandler authCodeHandler;
  private final AuthorizationCodeGrantType grantType;
  private final Map<Field, String> callbackValues;

  public AuthorizationCodeConnectionProviderObjectBuilder(ConnectionProviderModel providerModel,
                                                          ResolverSet resolverSet,
                                                          PoolingProfile poolingProfile,
                                                          ReconnectionConfig reconnectionConfig,
                                                          AuthorizationCodeGrantType grantType,
                                                          AuthorizationCodeOAuthHandler authCodeHandler,
                                                          ExtensionModel extensionModel,
                                                          ExpressionManager expressionManager,
                                                          MuleContext muleContext) {
    super(providerModel, resolverSet, poolingProfile, reconnectionConfig, extensionModel, expressionManager, muleContext);
    this.authCodeHandler = authCodeHandler;
    this.grantType = grantType;
    callbackValues = getCallbackValuesExtractors(providerModel);
  }

  @Override
  public void start() throws MuleException {
    authCodeHandler.register(getInitialOAuthConfig());
  }

  @Override
  protected ConnectionProvider<C> doBuild(ResolverSetResult result) throws MuleException {
    ConnectionProvider<C> provider = super.doBuild(result);

    Map<String, String> authCodeParams = getAuthCodeParameterMap(result);

    CustomOAuthParameters customParameters = getCustomParameters(result);
    AuthorizationCodeConfig config = new AuthorizationCodeConfig(ownerConfigName,
                                                                 buildOAuthObjectStoreConfig(result),
                                                                 customParameters.getQueryParams(),
                                                                 customParameters.getHeaders(),
                                                                 callbackValues,
                                                                 grantType,
                                                                 buildOAuthCallbackConfig(result),
                                                                 authCodeParams.get(CONSUMER_KEY_PARAMETER_NAME),
                                                                 authCodeParams.get(CONSUMER_SECRET_PARAMETER_NAME),
                                                                 authCodeParams.get(AUTHORIZATION_URL_PARAMETER_NAME),
                                                                 authCodeParams.get(ACCESS_TOKEN_URL_PARAMETER_NAME),
                                                                 authCodeParams.get(SCOPES_PARAMETER_NAME),
                                                                 authCodeParams.get(RESOURCE_OWNER_ID_PARAMETER_NAME),
                                                                 authCodeParams.get(BEFORE_FLOW_PARAMETER_NAME),
                                                                 authCodeParams.get(AFTER_FLOW_PARAMETER_NAME));

    provider = new AuthorizationCodeConnectionProviderWrapper<>(provider,
                                                                config,
                                                                callbackValues,
                                                                authCodeHandler,
                                                                reconnectionConfig);
    return provider;
  }

  @Override
  public Pair<ConnectionProvider<C>, ResolverSetResult> build(ValueResolvingContext context) throws MuleException {
    ResolverSetResult result = resolverSet.resolve(context);
    ConnectionProvider<C> provider = super.doBuild(result);

    Map<String, String> authCodeParams = getAuthCodeParameterMap(context.getEvent());

    CustomOAuthParameters customParameters = getCustomParameters(result);
    AuthorizationCodeConfig config = new AuthorizationCodeConfig(ownerConfigName,
                                                                 buildOAuthObjectStoreConfig(context.getEvent()),
                                                                 customParameters.getQueryParams(),
                                                                 customParameters.getHeaders(),
                                                                 callbackValues,
                                                                 grantType,
                                                                 buildOAuthCallbackConfig(context.getEvent()),
                                                                 authCodeParams.get(CONSUMER_KEY_PARAMETER_NAME),
                                                                 authCodeParams.get(CONSUMER_SECRET_PARAMETER_NAME),
                                                                 authCodeParams.get(AUTHORIZATION_URL_PARAMETER_NAME),
                                                                 authCodeParams.get(ACCESS_TOKEN_URL_PARAMETER_NAME),
                                                                 authCodeParams.get(SCOPES_PARAMETER_NAME),
                                                                 authCodeParams.get(RESOURCE_OWNER_ID_PARAMETER_NAME),
                                                                 authCodeParams.get(BEFORE_FLOW_PARAMETER_NAME),
                                                                 authCodeParams.get(AFTER_FLOW_PARAMETER_NAME));

    provider = new AuthorizationCodeConnectionProviderWrapper<>(provider,
                                                                config,
                                                                callbackValues,
                                                                authCodeHandler,
                                                                reconnectionConfig);
    return new Pair<>(provider, result);
  }

  private Map<String, String> getAuthCodeParameterMap(ResolverSetResult result) {
    return (Map<String, String>) result.get(OAUTH_AUTHORIZATION_CODE_GROUP_NAME);
  }

  private Map<String, String> getAuthCodeParameterMap(CoreEvent event) throws MuleException {
    ValueResolver<?> valueResolver = resolverSet.getResolvers().get(OAUTH_AUTHORIZATION_CODE_GROUP_NAME);
    try (ValueResolvingContext context = getResolvingContextFor(event)) {
      return (Map<String, String>) valueResolver.resolve(context);
    }
  }

  private OAuthCallbackConfig buildOAuthCallbackConfig(CoreEvent event) throws MuleException {
    ValueResolver<?> valueResolver = resolverSet.getResolvers().get(OAUTH_CALLBACK_GROUP_NAME);
    try (ValueResolvingContext context = getResolvingContextFor(event)) {
      Map<String, Object> map = (Map<String, Object>) valueResolver.resolve(context);
      return buildOAuthCallbackConfig(map);
    }
  }

  private OAuthCallbackConfig buildOAuthCallbackConfig(ResolverSetResult result) {
    Map<String, Object> map = (Map<String, Object>) result.get(OAUTH_CALLBACK_GROUP_NAME);
    return buildOAuthCallbackConfig(map);
  }

  private OAuthCallbackConfig buildOAuthCallbackConfig(Map<String, Object> map) {
    return new OAuthCallbackConfig((String) map.get(LISTENER_CONFIG_PARAMETER_NAME),
                                   sanitizePath((String) map.get(CALLBACK_PATH_PARAMETER_NAME)),
                                   sanitizePath((String) map.get(LOCAL_AUTHORIZE_PATH_PARAMETER_NAME)),
                                   (String) map.get(EXTERNAL_CALLBACK_URL_PARAMETER_NAME));
  }

  private AuthorizationCodeConfig getInitialOAuthConfig() throws MuleException {
    CoreEvent initialiserEvent = null;
    ValueResolvingContext ctxForConfig = null;
    ValueResolvingContext ctxForCallback = null;
    try {
      initialiserEvent = getNullEvent(muleContext);
      ValueResolver<?> oauthAuthCodeGroup = resolverSet.getResolvers().get(OAUTH_AUTHORIZATION_CODE_GROUP_NAME);

      MapValueResolver mapResolver = staticOnly((MapValueResolver) oauthAuthCodeGroup);
      Map<String, String> authCodeParams = mapResolver.resolve(ctxForConfig);

      ctxForConfig = getResolvingContextFor(initialiserEvent);
      Optional<OAuthObjectStoreConfig> storeConfig = buildOAuthObjectStoreConfig(initialiserEvent);

      mapResolver = staticOnly((MapValueResolver) resolverSet.getResolvers().get(OAUTH_CALLBACK_GROUP_NAME));
      ctxForCallback = getResolvingContextFor(initialiserEvent);
      OAuthCallbackConfig callbackConfig = buildOAuthCallbackConfig(mapResolver.resolve(ctxForCallback));

      CustomOAuthParameters customParameters = getCustomParameters(initialiserEvent);
      return new AuthorizationCodeConfig(ownerConfigName,
                                         storeConfig,
                                         customParameters.getQueryParams(),
                                         customParameters.getHeaders(),
                                         callbackValues,
                                         grantType,
                                         callbackConfig,
                                         authCodeParams.get(CONSUMER_KEY_PARAMETER_NAME),
                                         authCodeParams.get(CONSUMER_SECRET_PARAMETER_NAME),
                                         authCodeParams.get(AUTHORIZATION_URL_PARAMETER_NAME),
                                         authCodeParams.get(ACCESS_TOKEN_URL_PARAMETER_NAME),
                                         authCodeParams.get(SCOPES_PARAMETER_NAME),
                                         authCodeParams.get(RESOURCE_OWNER_ID_PARAMETER_NAME),
                                         authCodeParams.get(BEFORE_FLOW_PARAMETER_NAME),
                                         authCodeParams.get(AFTER_FLOW_PARAMETER_NAME));
    } finally {
      if (initialiserEvent != null) {
        ((BaseEventContext) initialiserEvent.getContext()).success();
      }
      if (ctxForCallback != null) {
        ctxForCallback.close();
      }
      if (ctxForConfig != null) {
        ctxForConfig.close();
      }
    }
  }
}
