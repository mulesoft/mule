/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

/**
 * Bean definition creation request. Provides all the required content to build a
 * {@link org.springframework.beans.factory.config.BeanDefinition}.
 *
 * @since 4.0
 */
public class CreateBeanDefinitionRequest {

  private final ComponentModel parentComponentModel;
  private final SpringComponentModel componentModel;
  private final ComponentBuildingDefinition componentBuildingDefinition;

  /**
   * @param parentComponentModel the parent component model. Some language construct processing depends on the context.
   * @param componentModel the holder for the configuration attributes defined by the user
   * @param componentBuildingDefinition the definition to build the domain object that will represent the configuration on runtime
   */
  public CreateBeanDefinitionRequest(ComponentModel parentComponentModel, SpringComponentModel componentModel,
                                     ComponentBuildingDefinition componentBuildingDefinition) {
    this.parentComponentModel = parentComponentModel;
    this.componentModel = componentModel;
    this.componentBuildingDefinition = componentBuildingDefinition;
  }


  public SpringComponentModel getComponentModel() {
    return componentModel;
  }

  public ComponentBuildingDefinition getComponentBuildingDefinition() {
    return componentBuildingDefinition;
  }

  public ComponentModel getParentComponentModel() {
    return parentComponentModel;
  }
}
