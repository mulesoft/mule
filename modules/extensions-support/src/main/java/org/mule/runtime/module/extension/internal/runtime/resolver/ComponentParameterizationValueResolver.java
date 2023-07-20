/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetUtils.getValueResolverFromComponentParameter;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.resolver.ValueResolverFactory;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

/**
 * A {@link ValueResolver} which resolves the value from a {@link ComponentParameterization}.
 *
 * @since 4.5
 */
public class ComponentParameterizationValueResolver<T> implements ValueResolver<T> {

  private final ParameterModel parameterModel;
  private final ReflectionCache reflectionCache;
  private final MuleContext muleContext;
  private final ValueResolverFactory valueResolverFactory;

  public ComponentParameterizationValueResolver(ParameterModel parameterModel,
                                                ReflectionCache reflectionCache,
                                                MuleContext muleContext,
                                                ValueResolverFactory valueResolverFactory) {
    this.parameterModel = parameterModel;
    this.reflectionCache = reflectionCache;
    this.muleContext = muleContext;
    this.valueResolverFactory = valueResolverFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T resolve(ValueResolvingContext context) throws MuleException {
    // TODO: check if using properties for piggybacking the values is fine, otherwise replace with a specific mechanism.
    Object value = context.getProperty(parameterModel.getName());
    if (value == null) {
      return null;
    }

    ValueResolver<T> parameterValueResolver = getValueResolverFromComponentParameter(parameterModel,
                                                                                     value,
                                                                                     reflectionCache,
                                                                                     muleContext,
                                                                                     valueResolverFactory);
    return parameterValueResolver.resolve(context);
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isDynamic() {
    return true;
  }
}
