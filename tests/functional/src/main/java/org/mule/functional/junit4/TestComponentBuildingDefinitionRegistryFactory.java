/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.internal.DefaultComponentBuildingDefinitionRegistryFactory;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionRegistryFactory;

import java.util.Set;

/**
 * Test implementation of {@link ComponentBuildingDefinitionRegistryFactory} which creates instances of
 * {@link ComponentBuildingDefinitionRegistry} and return same instance every time create is called.
 * 
 * @since 4.4.0
 */
public class TestComponentBuildingDefinitionRegistryFactory implements ComponentBuildingDefinitionRegistryFactory {

  private final Object lock = new Object();
  private ComponentBuildingDefinitionRegistry instance;

  @Override
  public ComponentBuildingDefinitionRegistry create(Set<ExtensionModel> extensionModels) {
    if (instance == null) {
      synchronized (lock) {
        if (instance == null) {
          instance = new DefaultComponentBuildingDefinitionRegistryFactory().create(extensionModels);
        }
      }
    }
    return instance;
  }
}
