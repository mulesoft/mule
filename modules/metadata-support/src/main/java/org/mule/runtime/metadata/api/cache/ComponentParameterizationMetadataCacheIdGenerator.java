/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metadata.api.cache;

import org.mule.runtime.api.parameterization.ComponentParameterization;

import java.util.Optional;

public interface ComponentParameterizationMetadataCacheIdGenerator
    extends MetadataCacheIdGenerator<ComponentParameterization<?>> {

  /**
   * Calculates the {@link MetadataCacheId} required to identify the MetadataType associated to the parameter named
   * {@code parameterName} of the given {@code component}. This method will take into account the values of the configured
   * {@link org.mule.runtime.api.metadata.MetadataKey} to provide an unique identifier of the {@code component} attributes type
   * definition.
   * 
   * @param parameterization   the component parametrization to calculate the metadata key from.
   * @param parameterGroupName the name of the parameter group
   * @param parameterName      the name of the parameter
   * @return a {@link MetadataCacheId} that identifies the component's parameter type with all its current configuration or
   *         {@link Optional#empty} if no valid identifier can be created.
   */
  Optional<MetadataCacheId> getIdForComponentInputMetadata(ComponentParameterization<?> parameterization,
                                                           String parameterGroupName,
                                                           String parameterName);

}
