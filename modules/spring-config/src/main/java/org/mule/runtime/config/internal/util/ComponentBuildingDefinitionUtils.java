/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.internal.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.internal.MuleArtifactContext;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.module.extension.internal.config.ExtensionBuildingDefinitionProvider;

/**
 * Utility class for registering {@link ComponentBuildingDefinition}.
 */
public final class ComponentBuildingDefinitionUtils {

  /**
   * Retrieve the {@link ComponentBuildingDefinition}s from a set of extension models.
   *
   * @param serviceRegistry the registry to use to find the {@link ExtensionBuildingDefinitionProvider}.
   * @param extensionModels the set of extension models.
   * @return the list of {@link ComponentBuildingDefinition}s to be used for creating components declared by the extension models.
   */
  public static List<ComponentBuildingDefinition> getExtensionModelsComponentBuildingDefinitions(ServiceRegistry serviceRegistry,
                                                                                                 Set<ExtensionModel> extensionModels) {
    List<ComponentBuildingDefinition> componentBuildingDefinitions = new ArrayList<>();
    serviceRegistry.lookupProviders(ComponentBuildingDefinitionProvider.class, MuleArtifactContext.class.getClassLoader())
        .forEach(componentBuildingDefinitionProvider -> {
          if (componentBuildingDefinitionProvider instanceof ExtensionBuildingDefinitionProvider) {
            ExtensionBuildingDefinitionProvider extensionBuildingDefinitionProvider =
                (ExtensionBuildingDefinitionProvider) componentBuildingDefinitionProvider;
            extensionBuildingDefinitionProvider
                .setExtensionModels(extensionModels);
            extensionBuildingDefinitionProvider.init();
            componentBuildingDefinitions.addAll(extensionBuildingDefinitionProvider.getComponentBuildingDefinitions());
          }
        });
    return componentBuildingDefinitions;
  }

  /**
   * Lookups for specific instances of {@link ComponentBuildingDefinitionProvider} declared in the provided class loader and
   * retrieves all the {@link ComponentBuildingDefinition}s from those providers.
   * <p/>
   * This method should be used to lookup for {@link ComponentBuildingDefinitionProvider}s custom implementations and not for the
   * ones provided when using the Mule SDK.
   *
   * @param serviceRegistry the registry to use to find the {@link ExtensionBuildingDefinitionProvider}.
   * @param classLoader the artifact class loader used for searching instances of {@link ComponentBuildingDefinitionProvider}s.
   * @return the list of {@link ComponentBuildingDefinition}s provided by the artifact class loader.
   */
  public static List<ComponentBuildingDefinition> getArtifactComponentBuildingDefinitions(ServiceRegistry serviceRegistry,
                                                                                          ClassLoader classLoader) {
    List<ComponentBuildingDefinition> componentBuildingDefinitions = new ArrayList<>();
    serviceRegistry.lookupProviders(ComponentBuildingDefinitionProvider.class, classLoader)
        .forEach(componentBuildingDefinitionProvider -> {
          if (componentBuildingDefinitionProvider.getClass().getClassLoader().equals(classLoader)) {
            componentBuildingDefinitionProvider.init();
            componentBuildingDefinitions.addAll(componentBuildingDefinitionProvider.getComponentBuildingDefinitions());
          }
        });
    return componentBuildingDefinitions;
  }

  public static void setRuntimeComponentBuildingDefinitions(ComponentBuildingDefinitionProvider runtimeComponentBuildingDefinitions) {
    ComponentBuildingDefinitionUtils.runtimeComponentBuildingDefinitions = runtimeComponentBuildingDefinitions;
  }
}
