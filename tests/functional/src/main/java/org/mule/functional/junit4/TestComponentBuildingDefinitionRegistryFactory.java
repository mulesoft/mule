/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.internal.DefaultComponentBuildingDefinitionRegistryFactory;

import org.mule.runtime.config.internal.model.ComponentBuildingDefinitionRegistryFactory;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test implementation of {@link ComponentBuildingDefinitionRegistryFactory} which cache created factory and return same instance
 * by extension models collection.
 * 
 * @since 4.4.0
 */
public class TestComponentBuildingDefinitionRegistryFactory implements ComponentBuildingDefinitionRegistryFactory {

  private static final Logger LOGGER = getLogger(TestComponentBuildingDefinitionRegistryFactory.class);
  private static final String EMPTY_KEY = "empty-key";
  private static final String NULL_KEY = "null-key";
  private static final String EXTENSION_SEPARATOR = ",";

  private final Map<String, ComponentBuildingDefinitionRegistry> registries = new ConcurrentHashMap<>();
  private boolean refreshRuntimeComponentBuildingDefinitions = false;

  @Override
  public ComponentBuildingDefinitionRegistry create(Set<ExtensionModel> extensionModels) {
    String key = getExtensionsKey(extensionModels);

    return registries.computeIfAbsent(key, k -> {
      LOGGER.info(format("Creating new ComponentBuildingDefinitionRegistryFactory for key: %s", key));
      DefaultComponentBuildingDefinitionRegistryFactory registryFactory = new DefaultComponentBuildingDefinitionRegistryFactory();
      if (refreshRuntimeComponentBuildingDefinitions) {
        registryFactory.refreshRuntimeComponentBuildingDefinitions();
      }
      return registryFactory.create(extensionModels);
    });
  }

  private String getExtensionsKey(Set<ExtensionModel> extensionModels) {
    if (extensionModels == null) {
      return NULL_KEY;
    } else {
      if (extensionModels.isEmpty()) {
        return EMPTY_KEY;
      }
      return extensionModels.stream().map(ExtensionModel::getName).sorted().collect(joining(EXTENSION_SEPARATOR));
    }
  }

    public void setRefreshRuntimeComponentBuildingDefinitions(boolean refreshRuntimeComponentBuildingDefinitions) {
      this.refreshRuntimeComponentBuildingDefinitions = refreshRuntimeComponentBuildingDefinitions;
    }
}
