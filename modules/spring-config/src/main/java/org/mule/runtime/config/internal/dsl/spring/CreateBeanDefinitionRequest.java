/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

/**
 * Bean definition creation request. Provides all the required content to build a
 * {@link org.springframework.beans.factory.config.BeanDefinition}.
 *
 * @since 4.0
 */
public class CreateBeanDefinitionRequest {

  private final SpringComponentModel componentModel;
  private final ComponentBuildingDefinition componentBuildingDefinition;

  /**
   * @param componentModel the holder for the configuration attributes defined by the user
   * @param componentBuildingDefinition the definition to build the domain object that will represent the configuration on runtime
   */
  public CreateBeanDefinitionRequest(SpringComponentModel componentModel,
                                     ComponentBuildingDefinition componentBuildingDefinition) {
    this.componentModel = componentModel;
    this.componentBuildingDefinition = componentBuildingDefinition;
  }


  public SpringComponentModel getComponentModel() {
    return componentModel;
  }

  public ComponentBuildingDefinition getComponentBuildingDefinition() {
    return componentBuildingDefinition;
  }
}
