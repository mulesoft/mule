/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
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
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.OAUTH_STORE_CONFIG_GROUP_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.OBJECT_STORE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.RESOURCE_OWNER_ID_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.SCOPES_PARAMETER_NAME;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext.from;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.util.StringMessageUtils;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthModelProperty;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.oauth.OAuthCallbackValuesModelProperty;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.MapValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;

/**
 * A specialization of {@link DefaultConnectionProviderObjectBuilder} to wrap the {@link ConnectionProvider}
 * into {@link OAuthConnectionProviderWrapper} instances.
 *
 * @since 4.0
 */
public class OAuthConnectionProviderObjectBuilder<C> extends DefaultConnectionProviderObjectBuilder<C> implements Startable {

  private final ExtensionsOAuthManager oauthManager;
  private final AuthorizationCodeGrantType grantType;
  private final Map<Field, String> callbackValues;

  public OAuthConnectionProviderObjectBuilder(ConnectionProviderModel providerModel,
                                              ResolverSet resolverSet,
                                              PoolingProfile poolingProfile,
                                              ReconnectionConfig reconnectionConfig,
                                              ExtensionsOAuthManager oauthManager,
                                              ExtensionModel extensionModel,
                                              MuleContext muleContext) {
    super(providerModel, resolverSet, poolingProfile, reconnectionConfig, extensionModel, muleContext);
    this.oauthManager = oauthManager;
    grantType = getGrantType();
    callbackValues = getCallbackValues();
  }

  @Override
  public void start() throws MuleException {
    oauthManager.register(getInitialOAuthConfig());
  }

  @Override
  protected ConnectionProvider<C> doBuild(ResolverSetResult result) throws MuleException {
    ConnectionProvider<C> provider = super.doBuild(result);

    OAuthConfig config = new OAuthConfig(ownerConfigName,
                                         buildAuthCodeConfig(result),
                                         buildOAuthCallbackConfig(result),
                                         buildOAuthObjectStoreConfig(result),
                                         grantType,
                                         getCustomParameters(result),
                                         getCallbackValues());

    provider = new OAuthConnectionProviderWrapper<>(provider,
                                                    config,
                                                    getCallbackValues(),
                                                    oauthManager,
                                                    reconnectionConfig);
    return provider;
  }

  @Override
  public Pair<ConnectionProvider<C>, ResolverSetResult> build(ValueResolvingContext context) throws MuleException {
    ResolverSetResult result = resolverSet.resolve(context);
    ConnectionProvider<C> provider = super.doBuild(result);

    OAuthConfig config = new OAuthConfig(ownerConfigName,
                                         buildAuthCodeConfig(context.getEvent()),
                                         buildOAuthCallbackConfig(context.getEvent()),
                                         buildOAuthObjectStoreConfig(context.getEvent()),
                                         grantType,
                                         getCustomParameters(result),
                                         getCallbackValues());

    provider = new OAuthConnectionProviderWrapper<>(provider,
                                                    config,
                                                    getCallbackValues(),
                                                    oauthManager,
                                                    reconnectionConfig);
    return new Pair<>(provider, result);
  }

  private AuthCodeConfig buildAuthCodeConfig(ResolverSetResult result) throws MuleException {
    Map<String, Object> map = (Map<String, Object>) result.get(OAUTH_AUTHORIZATION_CODE_GROUP_NAME);
    return buildAuthCodeConfig(map);
  }

  private AuthCodeConfig buildAuthCodeConfig(CoreEvent event) throws MuleException {
    Map<String, Object> map =
        (Map<String, Object>) resolverSet.getResolvers().get(OAUTH_AUTHORIZATION_CODE_GROUP_NAME).resolve(from(event));
    return buildAuthCodeConfig(map);
  }

  private AuthCodeConfig buildAuthCodeConfig(Map<String, Object> map) {
    return new AuthCodeConfig((String) map.get(CONSUMER_KEY_PARAMETER_NAME),
                              (String) map.get(CONSUMER_SECRET_PARAMETER_NAME),
                              (String) map.get(AUTHORIZATION_URL_PARAMETER_NAME),
                              (String) map.get(ACCESS_TOKEN_URL_PARAMETER_NAME),
                              (String) map.get(SCOPES_PARAMETER_NAME),
                              (String) map.get(RESOURCE_OWNER_ID_PARAMETER_NAME),
                              (String) map.get(BEFORE_FLOW_PARAMETER_NAME),
                              (String) map.get(AFTER_FLOW_PARAMETER_NAME));
  }

  private OAuthCallbackConfig buildOAuthCallbackConfig(CoreEvent event) throws MuleException {
    Map<String, Object> map =
        (Map<String, Object>) resolverSet.getResolvers().get(OAUTH_CALLBACK_GROUP_NAME).resolve(from(event));
    return buildOAuthCallbackConfig(map);
  }

  private OAuthCallbackConfig buildOAuthCallbackConfig(ResolverSetResult result) throws MuleException {
    Map<String, Object> map = (Map<String, Object>) result.get(OAUTH_CALLBACK_GROUP_NAME);
    return buildOAuthCallbackConfig(map);
  }

  private OAuthCallbackConfig buildOAuthCallbackConfig(Map<String, Object> map) {
    return new OAuthCallbackConfig((String) map.get(LISTENER_CONFIG_PARAMETER_NAME),
                                   sanitizePath((String) map.get(CALLBACK_PATH_PARAMETER_NAME)),
                                   sanitizePath((String) map.get(LOCAL_AUTHORIZE_PATH_PARAMETER_NAME)),
                                   (String) map.get(EXTERNAL_CALLBACK_URL_PARAMETER_NAME));
  }

  private Optional<OAuthObjectStoreConfig> buildOAuthObjectStoreConfig(CoreEvent event) throws MuleException {
    final ValueResolver resolver = resolverSet.getResolvers().get(OAUTH_STORE_CONFIG_GROUP_NAME);
    if (resolver == null) {
      return empty();
    }

    Map<String, Object> map = (Map<String, Object>) resolver.resolve(from(event));
    return map != null
        ? of(new OAuthObjectStoreConfig((String) map.get(OBJECT_STORE_PARAMETER_NAME)))
        : empty();
  }

  private Optional<OAuthObjectStoreConfig> buildOAuthObjectStoreConfig(ResolverSetResult result) throws MuleException {
    Map<String, Object> map = (Map<String, Object>) result.get(OAUTH_STORE_CONFIG_GROUP_NAME);
    return map != null
        ? of(new OAuthObjectStoreConfig((String) map.get(OBJECT_STORE_PARAMETER_NAME)))
        : empty();
  }


  private Map<Field, String> getCallbackValues() {
    return providerModel.getModelProperty(OAuthCallbackValuesModelProperty.class)
        .map(OAuthCallbackValuesModelProperty::getCallbackValues)
        .orElseGet(Collections::emptyMap);
  }

  private Map<String, String> getCustomParameters(ResolverSetResult result) {
    Map<String, String> oauthParams = new HashMap<>();
    withCustomParameters((parameter, property) -> oauthParams.put(property.getRequestAlias(),
                                                                  result.get(parameter.getName()).toString()));

    return oauthParams;
  }

  private void withCustomParameters(BiConsumer<ParameterModel, OAuthParameterModelProperty> delegate) {
    providerModel.getAllParameterModels().forEach(parameter -> parameter.getModelProperty(OAuthParameterModelProperty.class)
        .ifPresent(property -> delegate.accept(parameter, property)));
  }

  private Map<String, String> getCustomParameters(CoreEvent event) {
    Map<String, String> oauthParams = new HashMap<>();
    withCustomParameters((parameter, property) -> {
      String alias = property.getRequestAlias();
      if (StringUtils.isBlank(alias)) {
        alias = parameter.getName();
      }

      ValueResolver resolver = resolverSet.getResolvers().get(alias);
      if (resolver != null) {
        try {
          oauthParams.put(alias, resolveString(event, resolver));
        } catch (MuleException e) {
          throw new MuleRuntimeException(e);
        }
      }
    });

    return oauthParams;
  }

  private String resolveString(CoreEvent event, ValueResolver resolver) throws MuleException {
    Object value = resolver.resolve(from(event));
    return value != null ? StringMessageUtils.toString(value) : null;
  }

  private AuthorizationCodeGrantType getGrantType() {
    return providerModel.getModelProperty(OAuthModelProperty.class)
        .map(p -> (AuthorizationCodeGrantType) p.getGrantTypes().get(0))
        .get();
  }

  private String sanitizePath(String path) {
    return !path.startsWith("/") ? "/" + path : path;
  }

  private OAuthConfig getInitialOAuthConfig() throws MuleException {
    final CoreEvent initialiserEvent = getInitialiserEvent();
    MapValueResolver mapResolver =
        staticOnly((MapValueResolver) resolverSet.getResolvers().get(OAUTH_AUTHORIZATION_CODE_GROUP_NAME));
    AuthCodeConfig authCodeConfig = buildAuthCodeConfig(mapResolver.resolve(from(initialiserEvent)));
    Optional<OAuthObjectStoreConfig> storeConfig = buildOAuthObjectStoreConfig(initialiserEvent);

    mapResolver = staticOnly((MapValueResolver) resolverSet.getResolvers().get(OAUTH_CALLBACK_GROUP_NAME));
    OAuthCallbackConfig callbackConfig = buildOAuthCallbackConfig(mapResolver.resolve(from(initialiserEvent)));

    return new OAuthConfig(ownerConfigName,
                           authCodeConfig,
                           callbackConfig,
                           storeConfig,
                           grantType,
                           getCustomParameters(initialiserEvent),
                           callbackValues);
  }

  private MapValueResolver staticOnly(MapValueResolver resolver) throws MuleException {
    List<ValueResolver> staticKeyResolvers = new ArrayList<>(resolver.getKeyResolvers().size());
    List<ValueResolver> staticValueResolvers = new ArrayList<>(resolver.getValueResolvers().size());

    Iterator<ValueResolver> keyResolvers = resolver.getKeyResolvers().iterator();
    Iterator<ValueResolver> valueResolvers = resolver.getValueResolvers().iterator();

    while (keyResolvers.hasNext() && valueResolvers.hasNext()) {
      ValueResolver keyResolver = keyResolvers.next();
      ValueResolver valueResolver = valueResolvers.next();

      if (!keyResolver.isDynamic() && !valueResolver.isDynamic()) {
        staticKeyResolvers.add(keyResolver);
        staticValueResolvers.add(valueResolver);
      }
    }

    MapValueResolver result = new MapValueResolver(HashMap.class, staticKeyResolvers, staticValueResolvers, muleContext);
    initialiseIfNeeded(result, muleContext);

    return result;
  }
}
