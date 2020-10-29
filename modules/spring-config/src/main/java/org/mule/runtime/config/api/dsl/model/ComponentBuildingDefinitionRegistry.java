/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.config.internal.DefaultComponentBuildingDefinitionRegistry;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;

import java.util.Optional;

/**
 * Registry with all {@link ComponentBuildingDefinition} that where discovered in the classpath.
 * <p/>
 * {@code ComponentBuildingDefinition}s are located using SPI class {@link ComponentBuildingDefinitionProvider}.
 *
 * @since 4.0
 * @deprecated since 4.4, use the SDK instead of registering parsers manually.
 */
@Deprecated
public final class ComponentBuildingDefinitionRegistry {

  private DefaultComponentBuildingDefinitionRegistry delegate = new DefaultComponentBuildingDefinitionRegistry();

  /**
   * Adds a new {@code ComponentBuildingDefinition} to the registry.
   *
   * @param builderDefinition definition to be added in the registry
   */
  public void register(ComponentBuildingDefinition<?> builderDefinition) {
    delegate.register(builderDefinition);
  }

  /**
   * Lookups a {@code ComponentBuildingDefinition} for a certain configuration component.
   *
   * @param identifier the component identifier
   * @return the definition to build the component
   */
  public Optional<ComponentBuildingDefinition<?>> getBuildingDefinition(ComponentIdentifier identifier) {
    return delegate.getBuildingDefinition(identifier);
  }

  /**
   * Lookups a {@link WrapperElementType} for a certain configuration element.
   *
   * @param identifier the wrapper component identifier
   * @return the element type of the wrapper component
   */
  public Optional<WrapperElementType> getWrappedComponent(ComponentIdentifier identifier) {
    Optional<org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionRegistry.WrapperElementType> wrappedComponent =
        delegate.getWrappedComponent(identifier);
    return wrappedComponent.map(t -> WrapperElementType.valueOf(t.name()));
  }

  /**
   * Types of wrapper elements in the XML config.
   */
  public enum WrapperElementType {
    SINGLE, COLLECTION, MAP
  }
}
