/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.runtime.operation.OperationContext;
import org.mule.runtime.module.extension.internal.introspection.ParameterGroup;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterGroupArgumentResolver;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@link MetadataKeyObjectResolver} implementation for {@link OperationMessageProcessor}
 *
 * @since 4.0
 */
public class OperationMetadataKeyObjectResolver implements MetadataKeyObjectResolver {

  private final OperationModel operationModel;
  private final OperationContext operationContext;
  private ValueSupplier keyValueSupplier = () -> {
    throw new MetadataResolvingException("Cannot resolve the MetadataKey Id Value", FailureCode.UNKNOWN);
  };

  public OperationMetadataKeyObjectResolver(OperationContext operationContext) {
    this.operationContext = operationContext;
    this.operationModel = operationContext.getOperationModel();
    final Optional<MetadataKeyIdModelProperty> optionalKeyIdModelProperty =
        operationModel.getModelProperty(MetadataKeyIdModelProperty.class);

    if (optionalKeyIdModelProperty.isPresent()) {
      final MetadataKeyIdModelProperty keyIdModelProperty = optionalKeyIdModelProperty.get();
      final String parameterName = keyIdModelProperty.getParameterName();
      keyValueSupplier = isMultiLevelKey(keyIdModelProperty) ? () -> resolveMultiLevelMetadataKey(parameterName)
          : () -> resolveSingleLevelMetadataKey(parameterName);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getMetadataKeyValue() throws MetadataResolvingException {
    return keyValueSupplier.get();
  }

  private Object resolveMultiLevelMetadataKey(String parameterName) throws MetadataResolvingException {
    final ParameterGroupModelProperty parameterGroupModelProperty =
        operationModel.getModelProperty(ParameterGroupModelProperty.class).orElseThrow(RuntimeException::new);

    final ParameterGroup paramGroup = parameterGroupModelProperty.getGroups().stream()
        .filter(parameterGroup -> parameterGroup.getContainerName().equals(parameterName))
        .findFirst()
        .orElseThrow(() -> new MetadataResolvingException("Cannot resolve the MetadataKey Id Value. " +
            "Could not find the Parameter Group of the MetadataKey", FailureCode.UNKNOWN));

    return new ParameterGroupArgumentResolver<>(paramGroup).resolve(operationContext);
  }

  /**
   * @param parameterName the name of the parameter to resolve
   * @return the value of the parameter
   * @throws MetadataResolvingException if the resolution fails
   */
  private Object resolveSingleLevelMetadataKey(String parameterName) throws MetadataResolvingException {
    return operationContext.getParameter(parameterName);
  }

  private boolean isMultiLevelKey(MetadataKeyIdModelProperty componentModel) {
    return componentModel.getType() instanceof ObjectType;
  }

  /**
   * Utility interface, like {@link Supplier}, but declaring that it can throw a
   * {@link MetadataResolvingException} when supplying the value
   */
  private interface ValueSupplier {

    Object get() throws MetadataResolvingException;
  }
}
