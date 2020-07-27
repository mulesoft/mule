/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldValue;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getShowInDslParameters;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.ValueResolvingException;
import org.mule.runtime.module.extension.internal.runtime.config.ResolverSetBasedParameterResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterGroupArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.Map;
import java.util.Optional;

/**
 * {@link ParameterValueResolver} implementation for {@link OperationMessageProcessor}
 *
 * @since 4.0
 */
public final class OperationParameterValueResolver<T extends ComponentModel> implements ParameterValueResolver {

  private final T operationModel;
  private final ResolverSet resolverSet;
  private final ExpressionManager expressionManager;
  private final ExecutionContext<T> executionContext;
  private final Map<String, String> showInDslParameters;
  private final ReflectionCache reflectionCache;

  OperationParameterValueResolver(ExecutionContext<T> executionContext,
                                  ResolverSet resolverSet,
                                  ReflectionCache reflectionCache,
                                  ExpressionManager expressionManager) {
    this.executionContext = executionContext;
    this.operationModel = executionContext.getComponentModel();
    this.resolverSet = resolverSet;
    this.expressionManager = expressionManager;
    this.showInDslParameters = getShowInDslParameters(operationModel);
    this.reflectionCache = reflectionCache;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getParameterValue(String parameterName) throws ValueResolvingException {
    try {
      return getParameterGroup(parameterName)
          .map(group -> new ParameterGroupArgumentResolver<>(group, reflectionCache, expressionManager).resolve(executionContext))
          .orElseGet(() -> {
            String showInDslGroupName = showInDslParameters.get(parameterName);

            if (showInDslGroupName != null) {
              if (resolverSet.getResolvers().get(showInDslGroupName).isDynamic()) {
                try {
                  return new ResolverSetBasedParameterResolver(resolverSet, operationModel, reflectionCache, expressionManager)
                      .getParameterValue(parameterName);
                } catch (ValueResolvingException e) {
                  return null;
                }
              } else {
                return getShowInDslParameterValue(parameterName, showInDslGroupName);
              }
            }

            if (executionContext.hasParameter(parameterName)) {
              return executionContext.getParameter(parameterName);
            }

            return null;
          });
    } catch (Exception e) {
      throw new ValueResolvingException("Unable to resolve value for the parameter: " + parameterName, e);
    }
  }

  private Optional<ParameterGroupDescriptor> getParameterGroup(String parameterGroupName) {
    return operationModel.getParameterGroupModels().stream()
        // when resolving an inline group, we need to obtain it from the executionContext
        // and avoid its resolution using the ParameterGroupArgumentResolver
        // thus we filter all the groups that are shown in the dsl
        .filter(group -> group.getName().equals(parameterGroupName) && !group.isShowInDsl())
        .findFirst()
        .map(group -> group.getModelProperty(ParameterGroupModelProperty.class))
        .filter(Optional::isPresent)
        .map(group -> group.get().getDescriptor());
  }

  private Object getShowInDslParameterValue(String parameterName, String showInDslGroupName) {
    Object group = executionContext.getParameter(showInDslGroupName);
    try {
      return getFieldValue(group, parameterName, reflectionCache);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new IllegalStateException(
                                      format("An error occurred trying to obtain the field '%s' from the group '%s' of the Operation '%s'",
                                             parameterName, showInDslGroupName, operationModel.getName()));
    }
  }

  @Override
  public Map<String, ValueResolver<? extends Object>> getParameters() {
    return unmodifiableMap(resolverSet.getResolvers());
  }
}
