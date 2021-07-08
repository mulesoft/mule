/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import org.mule.runtime.api.component.ComponentIdentifier;
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
abstract class CreateBeanDefinitionRequest {

  private final List<ComponentAst> componentHierarchy;
  private final ComponentBuildingDefinition componentBuildingDefinition;
  private final SpringComponentModel springComponentModel;

  /**
   * @param parentComponentModel        the container element of the holder for the configuration attributes defined by the user
   * @param component                   the holder for the configuration attributes defined by the user
   * @param componentBuildingDefinition the definition to build the domain object that will represent the configuration on runtime
   */
  protected CreateBeanDefinitionRequest(List<ComponentAst> componentHierarchy,
                                        ComponentBuildingDefinition componentBuildingDefinition,
                                        ComponentIdentifier componentIdentifier) {
    this.componentHierarchy = componentHierarchy;
    this.componentBuildingDefinition = componentBuildingDefinition;
    this.springComponentModel = new SpringComponentModel();
    springComponentModel.setComponentIdentifier(componentIdentifier);

    ObjectTypeVisitor objectTypeVisitor = buildObjectTypeVisitor();

    if (componentBuildingDefinition != null) {
      componentBuildingDefinition.getTypeDefinition().visit(objectTypeVisitor);
    } else {
      objectTypeVisitor.onType(Object.class);
    }
    springComponentModel.setType(objectTypeVisitor.getType());
    objectTypeVisitor.getMapEntryType().ifPresent(springComponentModel::setMapEntryType);
  }

  protected ObjectTypeVisitor buildObjectTypeVisitor() {
    return new ObjectTypeVisitor(null);
  }

  public List<ComponentAst> getComponentHierarchy() {
    return componentHierarchy;
  }

  public ComponentBuildingDefinition getComponentBuildingDefinition() {
    return componentBuildingDefinition;
  }

  public SpringComponentModel getSpringComponentModel() {
    return springComponentModel;
  }

  /**
   * The {@link ComponentAst} to create a bean definition for this request
   */
  public abstract ComponentAst resolveConfigurationComponent();

  /**
   * The {@link SpringComponentModel} that represent the complex parameters of this request's component.
   */
  public abstract Collection<SpringComponentModel> getParamsModels();
}
