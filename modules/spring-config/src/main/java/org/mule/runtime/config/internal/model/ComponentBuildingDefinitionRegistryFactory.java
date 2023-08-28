/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.model;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;

import java.util.Set;

/**
 * Factory to create instances of {@link ComponentBuildingDefinitionRegistry}
 *
 * @since 4.4.0
 */
@NoImplement
public interface ComponentBuildingDefinitionRegistryFactory {

  /**
   * Creates a new {@link ComponentBuildingDefinitionRegistry}
   *
   * @param extensionModels the set of extension models.
   *
   * @return a non {@code null} {@link ComponentBuildingDefinitionRegistry}
   */
  public ComponentBuildingDefinitionRegistry create(Set<ExtensionModel> extensionModels);
}
