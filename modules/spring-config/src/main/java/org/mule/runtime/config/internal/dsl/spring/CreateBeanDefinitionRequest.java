/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
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
public abstract class CreateBeanDefinitionRequest<T> {

  private final List<ComponentAst> componentHierarchy;
  private final ComponentAst component;
  private final Collection<SpringComponentModel> paramsModels;
  private final ComponentBuildingDefinition<T> componentBuildingDefinition;
  private final SpringComponentModel springComponentModel;

  /**
   * @param parentComponentModel        the container element of the holder for the configuration attributes defined by the user
   * @param component                   the holder for the configuration attributes defined by the user
   * @param componentBuildingDefinition the definition to build the domain object that will represent the configuration on runtime
   */
  public CreateBeanDefinitionRequest(List<ComponentAst> componentHierarchy,
                                     ComponentAst component,
                                     Collection<SpringComponentModel> paramsModels,
                                     ComponentBuildingDefinition<T> componentBuildingDefinition,
                                     ComponentIdentifier componentIdentifier) {
    this.componentHierarchy = componentHierarchy;
    this.component = component;
    this.paramsModels = paramsModels;
    this.componentBuildingDefinition = componentBuildingDefinition;
    this.springComponentModel = new SpringComponentModel();
    springComponentModel.setComponentIdentifier(componentIdentifier);
    springComponentModel.setComponent(component);

    ObjectTypeVisitor objectTypeVisitor = new ObjectTypeVisitor(component);

    if (componentBuildingDefinition != null) {
      componentBuildingDefinition.getTypeDefinition().visit(objectTypeVisitor);
    } else {
      objectTypeVisitor.onType(Object.class);
    }
    springComponentModel.setType(objectTypeVisitor.getType());
    objectTypeVisitor.getMapEntryType().ifPresent(springComponentModel::setMapEntryType);
  }

  public List<ComponentAst> getComponentHierarchy() {
    return componentHierarchy;
  }

  public ComponentAst getComponent() {
    return component;
  }

  public Collection<SpringComponentModel> getParamsModels() {
    return paramsModels;
  }

  public ComponentBuildingDefinition<T> getComponentBuildingDefinition() {
    return componentBuildingDefinition;
  }

  public SpringComponentModel getSpringComponentModel() {
    return springComponentModel;
  }

  /**
   * The {@link ComponentAst} to create a bean definition for this request
   */
  public abstract ComponentAst resolveConfigurationComponent();

  protected ComponentAst resolveOwnerComponent() {
    for (int i = getComponentHierarchy().size() - 1; i >= 0; --i) {
      final ComponentAst possibleOwner = getComponentHierarchy().get(i);

      if (possibleOwner.getModel(ParameterizedModel.class).isPresent()) {
        return possibleOwner;
      }
    }

    return null;
  }

  /**
   * Resolve a parameter with the provided name in the scope of the param owner of this request.
   */
  public ComponentParameterAst getParameter(String parameterName) {
    // TODO MULE-19672 When decoupling from the dsl representation, properly propagate the group information to use here instead
    // of iterating.
    return getComponent().getParameters().stream()
        .filter(p -> p.getModel().getName().equals(parameterName))
        .findFirst()
        .orElse(null);
  }
}
