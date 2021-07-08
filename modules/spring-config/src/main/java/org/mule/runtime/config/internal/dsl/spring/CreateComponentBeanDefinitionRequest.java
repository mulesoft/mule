/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.util.Collections.emptyList;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.dsl.processor.ObjectTypeVisitor;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

import java.util.Collection;
import java.util.List;

class CreateComponentBeanDefinitionRequest extends CreateBeanDefinitionRequest {

  private final ComponentAst component;

  public CreateComponentBeanDefinitionRequest(List<ComponentAst> componentHierarchy,
                                              ComponentAst component,
                                              List<SpringComponentModel> paramsModels,
                                              ComponentBuildingDefinition componentBuildingDefinition) {
    super(componentHierarchy, componentBuildingDefinition, component.getIdentifier());

    this.component = component;
    getSpringComponentModel().setComponent(component);
  }

  @Override
  protected ObjectTypeVisitor buildObjectTypeVisitor() {
    return new ObjectTypeVisitor(component);
  }

  public ComponentAst getComponent() {
    return component;
  }

  @Override
  public ComponentAst resolveConfigurationComponent() {
    return getComponent();
  }

  @Override
  public Collection<SpringComponentModel> getParamsModels() {
    return emptyList();
  }
}
