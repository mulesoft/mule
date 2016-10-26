/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.operation.OperationContext;
import org.mule.runtime.module.extension.internal.introspection.ParameterGroup;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterGroupArgumentResolver;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link ParameterValueResolver} implementation for {@link OperationMessageProcessor}
 *
 * @since 4.0
 */
public class OperationParameterValueResolver implements ParameterValueResolver {

  private final OperationModel operationModel;
  private final OperationContext operationContext;

  public OperationParameterValueResolver(OperationContext operationContext) {
    this.operationContext = operationContext;
    this.operationModel = operationContext.getOperationModel();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getParameterValue(String containerName) {
    final Optional<ParameterGroup> parameterGroup = getParameterGroup(containerName);
    if (parameterGroup.isPresent()) {
      return new ParameterGroupArgumentResolver<>(parameterGroup.get()).resolve(operationContext);
    } else {
      return resolveSingleLevelMetadataKey(containerName);
    }
  }

  private Optional<ParameterGroup> getParameterGroup(String parameterGroupName) {
    final AtomicReference<ParameterGroup> atomicReference = new AtomicReference<>();

    operationModel.getModelProperty(ParameterGroupModelProperty.class)
        .ifPresent(paraGroup -> paraGroup.getGroups()
            .stream().filter(paramGroup -> paramGroup.getContainerName().equals(parameterGroupName))
            .findFirst()
            .ifPresent(atomicReference::set));
    return Optional.ofNullable(atomicReference.get());
  }

  /**
   * @param parameterName the name of the parameter to resolve
   * @return the value of the parameter
   */
  private Object resolveSingleLevelMetadataKey(String parameterName) {
    return operationContext.getParameter(parameterName);
  }
}
