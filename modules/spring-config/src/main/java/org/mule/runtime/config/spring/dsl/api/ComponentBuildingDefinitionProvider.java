/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.api;

import org.mule.runtime.core.api.MuleContext;

import java.util.List;

//TODO MULE-9646: move to an SPI package and remove from spring and move to API
/**
 * Service provider interface to define mule DSL extensions processors.
 *
 * During application config files processing all {@code ComponentBuildingDefinitionProvider} present in the classpath will be
 * lookup to search for available DSL parsing extensions.
 *
 * @since 4.0
 */
public interface ComponentBuildingDefinitionProvider {

  /**
   * Initialization method called once the extensions is discovered.
   *
   * @param muleContext the {@link org.mule.runtime.core.api.MuleContext} of the application being created.
   */
  void init(MuleContext muleContext);

  /**
   * @return list of {@code ComponentBuildingDefinition} provided by the extension.
   */
  List<ComponentBuildingDefinition> getComponentBuildingDefinitions();

}
