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
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.OAUTH_STORE_CONFIG_GROUP_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.OBJECT_STORE_PARAMETER_NAME;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.StringMessageUtils;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
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

public abstract class BaseOAuthConnectionProviderObjectBuilder<C> extends DefaultConnectionProviderObjectBuilder<C> {

  public BaseOAuthConnectionProviderObjectBuilder(ConnectionProviderModel providerModel,
                                                  ResolverSet resolverSet,
                                                  PoolingProfile poolingProfile,
                                                  ReconnectionConfig reconnectionConfig,
                                                  ExtensionModel extensionModel,
                                                  ExpressionManager expressionManager,
                                                  MuleContext muleContext) {
    super(providerModel, resolverSet, poolingProfile, reconnectionConfig, extensionModel, expressionManager, muleContext);
  }

  protected Optional<OAuthObjectStoreConfig> buildOAuthObjectStoreConfig(CoreEvent event) throws MuleException {
    final ValueResolver resolver = resolverSet.getResolvers().get(OAUTH_STORE_CONFIG_GROUP_NAME);
    if (resolver == null) {
      return empty();
    }

    try (ValueResolvingContext context = getResolvingContextFor(event)) {
      Map<String, Object> map = (Map<String, Object>) resolver.resolve(context);
      return map != null ? of(new OAuthObjectStoreConfig((String) map.get(OBJECT_STORE_PARAMETER_NAME))) : empty();
    }
  }

  protected Optional<OAuthObjectStoreConfig> buildOAuthObjectStoreConfig(ResolverSetResult result) {
    Map<String, Object> map = (Map<String, Object>) result.get(OAUTH_STORE_CONFIG_GROUP_NAME);
    return map != null ? of(new OAuthObjectStoreConfig((String) map.get(OBJECT_STORE_PARAMETER_NAME))) : empty();
  }


  protected void withCustomParameters(BiConsumer<ParameterModel, OAuthParameterModelProperty> delegate) {
    providerModel.getAllParameterModels().forEach(parameter -> parameter.getModelProperty(OAuthParameterModelProperty.class)
        .ifPresent(property -> delegate.accept(parameter, property)));
  }

  protected String resolveString(CoreEvent event, ValueResolver resolver) throws MuleException {
    try (ValueResolvingContext context = getResolvingContextFor(event)) {
      Object value = resolver.resolve(context);
      return value != null ? StringMessageUtils.toString(value) : null;
    }
  }

  protected ValueResolvingContext getResolvingContextFor(CoreEvent event) {
    return ValueResolvingContext.builder(event).withExpressionManager(expressionManager).build();
  }

  protected MapValueResolver staticOnly(MapValueResolver resolver) throws MuleException {
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

    MapValueResolver result =
        new MapValueResolver(HashMap.class, staticKeyResolvers, staticValueResolvers, getReflectionCache(), muleContext);
    initialiseIfNeeded(result, muleContext);

    return result;
  }

  protected Map<Field, String> getCallbackValues() {
    return providerModel.getModelProperty(OAuthCallbackValuesModelProperty.class)
        .map(OAuthCallbackValuesModelProperty::getCallbackValues)
        .orElseGet(Collections::emptyMap);
  }

  protected MultiMap<String, String> getCustomParameters(ResolverSetResult result) {
    MultiMap<String, String> oauthParams = new MultiMap<>();
    withCustomParameters((parameter, property) -> oauthParams.put(property.getRequestAlias(),
                                                                  result.get(parameter.getName()).toString()));
    return oauthParams;
  }

  protected String sanitizePath(String path) {
    return !path.startsWith("/") ? "/" + path : path;
  }

  protected MultiMap<String, String> getCustomParameters(CoreEvent event) {
    MultiMap<String, String> oauthParams = new MultiMap<>();
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
}
