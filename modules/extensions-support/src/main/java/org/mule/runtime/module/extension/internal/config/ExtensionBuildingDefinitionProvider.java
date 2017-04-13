/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;

import java.util.Set;

/**
 * Provider for building definitions for java based extensions.
 * 
 * @since 4.0
 */
public interface ExtensionBuildingDefinitionProvider extends ComponentBuildingDefinitionProvider {

  /**
   * Sets the artifact configured extensions to be used for generating the
   * {@link org.mule.runtime.dsl.api.component.ComponentBuildingDefinition}s for each of the components defined within the
   * extension.
   * <p>
   * This method is expected to be invoked before calling to {@link ComponentBuildingDefinitionProvider#init()}
   * 
   * @param extensionModels configured extensions within the artifact.
   */
  void setExtensionModels(Set<ExtensionModel> extensionModels);

}
