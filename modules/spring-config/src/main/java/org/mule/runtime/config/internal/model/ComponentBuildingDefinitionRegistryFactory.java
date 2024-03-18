/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
