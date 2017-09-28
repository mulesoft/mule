/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry with all {@link ComponentBuildingDefinition} that where discovered in the classpath.
 * <p/>
 * {@code ComponentBuildingDefinition}s are located using SPI class {@link ComponentBuildingDefinitionProvider}.
 *
 * @since 4.0
 */
public class ComponentBuildingDefinitionRegistry {

  private Map<ComponentIdentifier, ComponentBuildingDefinition<?>> builderDefinitionsMap = new HashMap<>();

  /**
   * Adds a new {@code ComponentBuildingDefinition} to the registry.
   *
   * @param builderDefinition definition to be added in the registry
   */
  public void register(ComponentBuildingDefinition<?> builderDefinition) {
    builderDefinitionsMap.put(builderDefinition.getComponentIdentifier(), builderDefinition);
  }

  /**
   * Lookups a {@code ComponentBuildingDefinition} for a certain configuration component.
   *
   * @param identifier the component identifier
   * @return the definition to build the component
   */
  public Optional<ComponentBuildingDefinition<?>> getBuildingDefinition(ComponentIdentifier identifier) {
    return Optional.ofNullable(builderDefinitionsMap.get(identifier));
  }
}
