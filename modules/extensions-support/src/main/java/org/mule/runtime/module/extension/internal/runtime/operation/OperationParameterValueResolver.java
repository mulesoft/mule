/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterGroupArgumentResolver;

import java.util.Optional;

/**
 * {@link ParameterValueResolver} implementation for {@link OperationMessageProcessor}
 *
 * @since 4.0
 */
public class OperationParameterValueResolver implements ParameterValueResolver {

  private final OperationModel operationModel;
  private final ExecutionContext<OperationModel> executionContext;

  public OperationParameterValueResolver(ExecutionContext<OperationModel> executionContext) {
    this.executionContext = executionContext;
    this.operationModel = executionContext.getComponentModel();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getParameterValue(String containerName) {
    return getParameterGroup(containerName)
        .map(group -> new ParameterGroupArgumentResolver<>(group).resolve(executionContext))
        .orElseGet(() -> executionContext.getParameter(containerName));
  }

  private Optional<ParameterGroupDescriptor> getParameterGroup(String parameterGroupName) {
    return operationModel.getParameterGroupModels().stream()
        .filter(group -> group.getName().equals(parameterGroupName))
        .findFirst()
        .map(group -> group.getModelProperty(ParameterGroupModelProperty.class))
        .filter(Optional::isPresent)
        .map(group -> group.get().getDescriptor());
  }
}
