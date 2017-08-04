/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.util;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.spring.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.module.extension.internal.config.ExtensionBuildingDefinitionProvider;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.emptySet;

public class ComponentBuildingDefinitionUtils {

  public static void registerComponentBuildingDefinitions(ServiceRegistry serviceRegistry, ClassLoader classLoader,
                                                          ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry,
                                                          Optional<Set<ExtensionModel>> extensionModels,
                                                          Function<ComponentBuildingDefinitionProvider, List<ComponentBuildingDefinition>> providerListFunction) {

    serviceRegistry.lookupProviders(ComponentBuildingDefinitionProvider.class, classLoader)
        .forEach(componentBuildingDefinitionProvider -> {
          boolean isExtensionBuildingDefinitionProvider =
              componentBuildingDefinitionProvider instanceof ExtensionBuildingDefinitionProvider;
          if (isExtensionBuildingDefinitionProvider) {
            ((ExtensionBuildingDefinitionProvider) componentBuildingDefinitionProvider)
                .setExtensionModels(extensionModels.orElse(emptySet()));
          }
          componentBuildingDefinitionProvider.init();

          List<ComponentBuildingDefinition> componentBuildingDefinitions =
              isExtensionBuildingDefinitionProvider ? providerListFunction.apply(componentBuildingDefinitionProvider)
                  : componentBuildingDefinitionProvider.getComponentBuildingDefinitions();

          componentBuildingDefinitions.forEach(componentBuildingDefinitionRegistry::register);
        });
  }

}
