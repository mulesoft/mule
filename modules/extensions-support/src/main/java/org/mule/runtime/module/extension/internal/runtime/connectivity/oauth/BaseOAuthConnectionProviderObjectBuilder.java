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
import static org.mule.runtime.extension.api.runtime.parameter.HttpParameterPlacement.HEADERS;
import static org.mule.runtime.extension.api.runtime.parameter.HttpParameterPlacement.QUERY_PARAMS;
import static org.mule.runtime.extension.api.runtime.parameter.HttpParameterPlacement.FORM_PARAMS;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.func.CheckedFunction;
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
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

/**
 * Base class for {@link DefaultConnectionProviderObjectBuilder} specializations which yield OAuth enabled connection
 * providers
 *
 * @param <C> the generic type of the connections to be produced
 * @since 4.2.1
 */
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

  protected Object resolve(CoreEvent event, ValueResolver resolver) throws MuleException {
    try (ValueResolvingContext context = getResolvingContextFor(event)) {
      return resolver.resolve(context);
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

  protected CustomOAuthParameters getCustomParameters(ResolverSetResult result) {
    return getCustomParameters(key -> result.get(key));
  }

  protected String sanitizePath(String path) {
    return !path.startsWith("/") ? "/" + path : path;
  }

  protected CustomOAuthParameters getCustomParameters(CoreEvent event) {
    return getCustomParameters((CheckedFunction<String, Object>) key -> {
      ValueResolver resolver = resolverSet.getResolvers().get(key);
      if (resolver != null) {
        return resolve(event, resolver);
      }

      return null;
    });
  }

  private CustomOAuthParameters getCustomParameters(Function<String, Object> valueFunction) {
    CustomOAuthParameters params = new CustomOAuthParameters();
    withCustomParameters((parameter, property) -> {
      String alias = property.getRequestAlias();
      if (StringUtils.isBlank(alias)) {
        alias = parameter.getName();
      }

      final Object value = valueFunction.apply(alias);

      if (value == null) {
        return;
      }

      final MultiMap<String, String> target;

      if (property.getPlacement() == QUERY_PARAMS) {
        target = params.getQueryParams();
      } else if (property.getPlacement() == HEADERS) {
        target = params.getHeaders();
      } else if (property.getPlacement() == FORM_PARAMS) {
        target = params.getFormParams();
      } else {
        throw new IllegalArgumentException("Unknown parameter placement: " + property.getPlacement());
      }

      if (value instanceof Map) {
        target.putAll((Map) value);
      } else if (value instanceof List) {
        target.put(alias, (List) value);
      } else {
        target.put(alias, value.toString());
      }
    });

    return params;
  }

  protected class CustomOAuthParameters {

    private MultiMap<String, String> queryParams = new MultiMap<>();
    private MultiMap<String, String> headers = new MultiMap<>();
    private MultiMap<String, String> formParams = new MultiMap<>();

    public MultiMap<String, String> getQueryParams() {
      return queryParams;
    }

    public MultiMap<String, String> getHeaders() {
      return headers;
    }

    public MultiMap<String, String> getFormParams() {
      return formParams;
    }
  }
}
