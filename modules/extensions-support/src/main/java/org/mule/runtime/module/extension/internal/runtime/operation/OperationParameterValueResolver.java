/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getContainerName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldValue;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getShowInDslParameters;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.util.Pair;
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
          .map(pair -> {
            if (!pair.getSecond().isShowInDsl()) {
              return new ParameterGroupArgumentResolver<>(pair.getFirst(), reflectionCache, expressionManager)
                  .resolve(executionContext);
            } else {
              String parameterGroupContainerName = getContainerName(pair.getFirst().getContainer());
              if (parameterGroupContainerName != null && executionContext.hasParameter(parameterGroupContainerName)) {
                return executionContext.getParameter(parameterGroupContainerName);
              }
              return null;
            }
          })
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

  private Optional<Pair<ParameterGroupDescriptor, ParameterGroupModel>> getParameterGroup(String parameterGroupName) {
    return operationModel.getParameterGroupModels().stream()
        .filter(parameterGroupModel -> parameterGroupModel.getName().equals(parameterGroupName))
        .findFirst()
        .map(parameterGroupModel -> {
          Optional<ParameterGroupModelProperty> parameterGroupModelModelProperty =
              parameterGroupModel.getModelProperty(ParameterGroupModelProperty.class);
          if (!parameterGroupModelModelProperty.isPresent()) {
            return null;
          }
          return new Pair(parameterGroupModelModelProperty.get().getDescriptor(), parameterGroupModel);
        });
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
