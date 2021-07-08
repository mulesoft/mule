/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.dsl.processor.ObjectTypeVisitor;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

import java.util.Collection;
import java.util.List;

/**
 * Bean definition creation request. Provides all the required content to build a
 * {@link org.springframework.beans.factory.config.BeanDefinition}.
 *
 * @since 4.0
 */
public class CreateComponentBeanDefinitionRequest implements CreateBeanDefinitionRequest {

  private final List<ComponentAst> componentModelHierarchy;
  private final ComponentAst componentModel;
  private final Collection<SpringComponentModel> paramsModels;
  private final ComponentBuildingDefinition componentBuildingDefinition;
  private final SpringComponentModel springComponentModel;

  /**
   * @param parentComponentModel        the container element of the holder for the configuration attributes defined by the user
   * @param componentModel              the holder for the configuration attributes defined by the user
   * @param componentBuildingDefinition the definition to build the domain object that will represent the configuration on runtime
   */
  public CreateComponentBeanDefinitionRequest(List<ComponentAst> componentModelHierarchy,
                                              ComponentAst componentModel,
                                              Collection<SpringComponentModel> paramsModels,
                                              ComponentBuildingDefinition componentBuildingDefinition) {
    this.componentModelHierarchy = componentModelHierarchy;
    this.componentModel = componentModel;
    this.paramsModels = paramsModels;
    this.componentBuildingDefinition = componentBuildingDefinition;
    this.springComponentModel = new SpringComponentModel();
    springComponentModel.setComponentIdentifier(componentModel.getIdentifier());
    springComponentModel.setComponent(componentModel);

    ObjectTypeVisitor typeVisitor = new ObjectTypeVisitor(componentModel);

    if (componentBuildingDefinition != null) {
      componentBuildingDefinition.getTypeDefinition().visit(typeVisitor);
    } else {
      typeVisitor.onType(Object.class);
    }

    springComponentModel.setType(typeVisitor.getType());
    typeVisitor.getMapEntryType().ifPresent(met -> getSpringComponentModel().setMapEntryType(met));
  }

  public List<ComponentAst> getComponentModelHierarchy() {
    return componentModelHierarchy;
  }

  public ComponentAst getComponentModel() {
    return componentModel;
  }

  @Override
  public Collection<SpringComponentModel> getParamsModels() {
    return paramsModels;
  }

  @Override
  public ComponentBuildingDefinition getComponentBuildingDefinition() {
    return componentBuildingDefinition;
  }

  @Override
  public SpringComponentModel getSpringComponentModel() {
    return springComponentModel;
  }
}
