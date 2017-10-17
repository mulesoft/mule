/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.lang.String.format;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldValue;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getGroupModelContainerName;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.ValueResolvingException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterGroupArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@link ParameterValueResolver} implementation for {@link OperationMessageProcessor}
 *
 * @since 4.0
 */
public final class OperationParameterValueResolver<T extends ComponentModel> implements ParameterValueResolver {

  private final T operationModel;
  private final ExecutionContext<T> executionContext;
  private final Map<String, String> showInDslParameters;

  OperationParameterValueResolver(ExecutionContext<T> executionContext) {
    this.executionContext = executionContext;
    this.operationModel = executionContext.getComponentModel();
    this.showInDslParameters = getShowInDslParameters();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getParameterValue(String parameterName) throws ValueResolvingException {
    try {
      return getParameterGroup(parameterName)
          .map(group -> new ParameterGroupArgumentResolver<>(group).resolve(executionContext))
          .orElseGet(() -> {
            String showInDslGroupName = showInDslParameters.get(parameterName);
            return showInDslGroupName != null
                ? getShowInDslParameterValue(parameterName, showInDslGroupName)
                : executionContext.getParameter(parameterName);
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
      return getFieldValue(group, parameterName);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new IllegalStateException(format("An error occurred trying to obtain the field '%s' from the group '%s' of the Operation '%s'",
                                             parameterName, showInDslGroupName, operationModel.getName()));
    }
  }

  private Map<String, String> getShowInDslParameters() {
    HashMap<String, String> showInDslMap = new HashMap<>();

    operationModel.getParameterGroupModels().stream()
        .filter(ParameterGroupModel::isShowInDsl)
        .forEach(groupModel -> groupModel.getParameterModels()
            .forEach(param -> showInDslMap.put(IntrospectionUtils.getImplementingName(param),
                                               getGroupModelContainerName(groupModel))));

    return showInDslMap;
  }
}
