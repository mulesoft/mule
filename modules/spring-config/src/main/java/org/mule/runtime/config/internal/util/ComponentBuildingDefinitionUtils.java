/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.internal.util;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.internal.MuleArtifactContext;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.module.extension.internal.config.ExtensionBuildingDefinitionProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility class for registering {@link ComponentBuildingDefinition}.
 */
public final class ComponentBuildingDefinitionUtils {

  private static final ServiceRegistry SERVICE_REGISTRY = new SpiServiceRegistry();

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
                           MuleArtifactContext.class.getClassLoader())
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

  /**
   * Retrieve the {@link ComponentBuildingDefinition}s from a set of extension models.
   *
   * @param extensionModels     the set of extension models.
   * @param dslResolvingContext
   * @return the list of {@link ComponentBuildingDefinition}s to be used for creating components declared by the extension models.
   */
  public static List<ComponentBuildingDefinition> getExtensionModelsComponentBuildingDefinitions(Set<ExtensionModel> extensionModels,
                                                                                                 DslResolvingContext dslResolvingContext) {
    List<ComponentBuildingDefinition> componentBuildingDefinitions = new ArrayList<>();
    SERVICE_REGISTRY.lookupProviders(ComponentBuildingDefinitionProvider.class, MuleArtifactContext.class.getClassLoader())
        .forEach(componentBuildingDefinitionProvider -> {
          if (componentBuildingDefinitionProvider instanceof ExtensionBuildingDefinitionProvider) {
            ExtensionBuildingDefinitionProvider extensionBuildingDefinitionProvider =
                (ExtensionBuildingDefinitionProvider) componentBuildingDefinitionProvider;
            extensionBuildingDefinitionProvider.setExtensionModels(extensionModels);
            extensionBuildingDefinitionProvider.setDslResolvingContext(dslResolvingContext);
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
   * @param classLoader the artifact class loader used for searching instances of {@link ComponentBuildingDefinitionProvider}s.
   * @return the list of {@link ComponentBuildingDefinition}s provided by the artifact class loader.
   */
  public static List<ComponentBuildingDefinition> getArtifactComponentBuildingDefinitions(ClassLoader classLoader) {
    List<ComponentBuildingDefinition> componentBuildingDefinitions = new ArrayList<>();
    SERVICE_REGISTRY.lookupProviders(ComponentBuildingDefinitionProvider.class, classLoader)
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
