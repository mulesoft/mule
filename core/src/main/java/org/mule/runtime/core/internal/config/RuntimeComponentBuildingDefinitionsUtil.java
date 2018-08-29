/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import java.util.ArrayList;
import java.util.List;

import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;

/**
 * Utility method for getting the runtime {@link ComponentBuildingDefinition}s.
 */
public class RuntimeComponentBuildingDefinitionsUtil {

  private static ComponentBuildingDefinitionProvider runtimeComponentBuildingDefinitions;

  /**
   * Utility method to find all {@link ComponentBuildingDefinition}s provided by the runtime.
   *
   * @return a {@link ComponentBuildingDefinitionProvider} containing all the runtime definitions.
   */
  public static ComponentBuildingDefinitionProvider getRuntimeComponentBuildingDefinitionProvider() {
    if (runtimeComponentBuildingDefinitions == null) {
      List<ComponentBuildingDefinition> allDefinitions = new ArrayList<>();
      new SpiServiceRegistry()
          .lookupProviders(ComponentBuildingDefinitionProvider.class,
                           RuntimeComponentBuildingDefinitionsUtil.class.getClassLoader())
          .forEach(componentBuildingDefinitionProvider -> {
            componentBuildingDefinitionProvider.init();

            allDefinitions.addAll(componentBuildingDefinitionProvider.getComponentBuildingDefinitions());
          });
      runtimeComponentBuildingDefinitions = new ComponentBuildingDefinitionProvider() {

        @Override
        public void init() {}

        @Override
        public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
          return allDefinitions;
        }
      };
    }
    return runtimeComponentBuildingDefinitions;
  }

}
