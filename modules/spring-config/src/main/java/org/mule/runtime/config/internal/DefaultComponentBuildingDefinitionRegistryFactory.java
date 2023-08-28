/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal;

import static org.mule.runtime.config.internal.util.ComponentBuildingDefinitionUtils.getArtifactComponentBuildingDefinitions;
import static org.mule.runtime.config.internal.util.ComponentBuildingDefinitionUtils.getExtensionModelsComponentBuildingDefinitions;
import static org.mule.runtime.config.internal.util.ComponentBuildingDefinitionUtils.getRuntimeComponentBuildingDefinitionProvider;
import static org.mule.runtime.module.artifact.activation.internal.classloader.MuleApplicationClassLoader.resolveContextArtifactPluginClassLoaders;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.internal.model.ComponentBuildingDefinitionRegistryFactory;
import org.mule.runtime.config.internal.util.ComponentBuildingDefinitionUtils;

import java.util.Set;

/**
 * Default implementation of {@link ComponentBuildingDefinitionRegistryFactory} which creates instances of
 * {@link ComponentBuildingDefinitionRegistry}
 *
 * @since 4.4.0
 */
public class DefaultComponentBuildingDefinitionRegistryFactory implements ComponentBuildingDefinitionRegistryFactory {

  @Override
  public ComponentBuildingDefinitionRegistry create(Set<ExtensionModel> extensionModels) {
    ComponentBuildingDefinitionRegistry registry = new ComponentBuildingDefinitionRegistry();

    getRuntimeComponentBuildingDefinitionProvider().getComponentBuildingDefinitions()
        .forEach(registry::register);

    if (extensionModels != null) {
      getExtensionModelsComponentBuildingDefinitions(extensionModels, DslResolvingContext.getDefault(extensionModels))
          .forEach(registry::register);
    }

    for (ClassLoader pluginArtifactClassLoader : resolveContextArtifactPluginClassLoaders()) {
      getArtifactComponentBuildingDefinitions(pluginArtifactClassLoader)
          .forEach(registry::register);
    }
    return registry;
  }

  public void refreshRuntimeComponentBuildingDefinitions() {
    ComponentBuildingDefinitionUtils.setRuntimeComponentBuildingDefinitions(null);
  }
}
