/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.config.internal.model.ComponentBuildingDefinitionRegistryFactory;

/**
 * Implementations may receive a {@link ComponentBuildingDefinitionRegistryFactory} to use.
 * 
 * @since 4.5
 */
public interface ComponentBuildingDefinitionRegistryFactoryAware {

  void setComponentBuildingDefinitionRegistryFactory(ComponentBuildingDefinitionRegistryFactory componentBuildingDefinitionRegistryFactory);
}
