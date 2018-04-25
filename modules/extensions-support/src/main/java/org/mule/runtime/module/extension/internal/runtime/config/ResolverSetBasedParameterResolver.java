/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.lang.String.format;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.module.extension.internal.runtime.ValueResolvingException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ObjectBasedParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.Map;
import java.util.Optional;

/**
 * {@link ParameterValueResolver} based on {@link ResolverSet resolver sets}
 *
 * @since 4.1.1
 */
public class ResolverSetBasedParameterResolver implements ParameterValueResolver {

  private ResolverSet resolverSet;
  private ParameterizedModel parameterizedModel;
  private ReflectionCache reflectionCache;
  private ValueResolvingContext resolvingContext = ValueResolvingContext.from(null);

  public ResolverSetBasedParameterResolver(ResolverSet resolverSet, ParameterizedModel parameterizedModel,
                                           ReflectionCache reflectionCache) {
    this.resolverSet = resolverSet;
    this.parameterizedModel = parameterizedModel;
    this.reflectionCache = reflectionCache;
  }

  @Override
  public Object getParameterValue(String parameterName)
      throws ValueResolvingException {
    try {
      ValueResolver<?> valueResolver = resolverSet.getResolvers().get(parameterName);
      if (valueResolver != null) {
        return valueResolver.resolve(resolvingContext);
      } else {
        return resolveFromParameterGroup(parameterName);
      }
    } catch (MuleException e) {
      throw new ValueResolvingException(format("Error occurred trying to resolve value for the parameter [%s]",
                                               parameterName));
    }
  }

  private Object resolveFromParameterGroup(String parameterName) throws ValueResolvingException, MuleException {
    Optional<? extends ValueResolver<?>> paramGroupValueResolver = getParameterGroupValueResolver(parameterName);
    if (paramGroupValueResolver.isPresent()) {
      ValueResolver<?> paramGroup = paramGroupValueResolver.get();
      return paramGroup.isDynamic()
          ? resolveDynamicGroup(parameterName, paramGroup)
          : resolveStaticGroup(parameterName, paramGroup);
    } else {
      throw new ValueResolvingException(format("An error occurred trying to resolve the parameter [%s]",
                                               parameterName));
    }
  }

  private Optional<? extends ValueResolver<?>> getParameterGroupValueResolver(String parameterName) {
    Map<String, String> showInDslParameters = IntrospectionUtils.getShowInDslParameters(parameterizedModel);
    String parameterGroupName = showInDslParameters.get(parameterName);
    ValueResolver<?> valueResolver = null;
    if (parameterGroupName != null) {
      valueResolver = resolverSet.getResolvers().get(parameterGroupName);
    }

    return Optional.ofNullable(valueResolver);
  }

  private Object resolveStaticGroup(String parameterName, ValueResolver<?> paramGroup)
      throws MuleException, ValueResolvingException {
    Object resolve = paramGroup.resolve(ValueResolvingContext.from(null));
    return new ObjectBasedParameterValueResolver(resolve, parameterizedModel, reflectionCache)
        .getParameterValue(parameterName);
  }

  private Object resolveDynamicGroup(String parameterName, ValueResolver<?> paramGroup) throws ValueResolvingException {
    if (paramGroup instanceof ParameterValueResolver) {
      return ((ParameterValueResolver) paramGroup).getParameterValue(parameterName);
    } else {
      throw new ValueResolvingException(format("An error occurred trying to resolve the parameter [%s]",
                                               parameterName));
    }
  }
}
