/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.dsl.processor.ObjectTypeVisitor;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

import java.util.List;
import java.util.function.Supplier;

/**
 * Bean definition creation request. Provides all the required content to build a
 * {@link org.springframework.beans.factory.config.BeanDefinition}.
 *
 * @since 4.0
 */
public class CreateBeanDefinitionRequest {

  private final List<ComponentAst> componentModelHierarchy;
  private final ComponentAst componentModel;
  private final ComponentBuildingDefinition componentBuildingDefinition;
  private final SpringComponentModel springComponentModel;
  private final Supplier<ObjectTypeVisitor> typeVisitorRetriever;

  /**
   * @param parentComponentModel        the container element of the holder for the configuration attributes defined by the user
   * @param componentModel              the holder for the configuration attributes defined by the user
   * @param componentBuildingDefinition the definition to build the domain object that will represent the configuration on runtime
   */
  public CreateBeanDefinitionRequest(List<ComponentAst> componentModelHierarchy,
                                     ComponentAst componentModel,
                                     ComponentBuildingDefinition componentBuildingDefinition) {
    this.componentModelHierarchy = componentModelHierarchy;
    this.componentModel = componentModel;
    this.componentBuildingDefinition = componentBuildingDefinition;
    this.springComponentModel = new SpringComponentModel();
    springComponentModel.setComponent(componentModel);

    this.typeVisitorRetriever = new LazyValue<>(() -> {
      ObjectTypeVisitor objectTypeVisitor = new ObjectTypeVisitor(componentModel);

      if (componentBuildingDefinition != null) {
        componentBuildingDefinition.getTypeDefinition().visit(objectTypeVisitor);
      } else {
        objectTypeVisitor.onType(Object.class);
      }

      return objectTypeVisitor;
    });
  }

  public List<ComponentAst> getComponentModelHierarchy() {
    return componentModelHierarchy;
  }

  public ComponentAst getComponentModel() {
    return componentModel;
  }

  public ComponentBuildingDefinition getComponentBuildingDefinition() {
    return componentBuildingDefinition;
  }

  public SpringComponentModel getSpringComponentModel() {
    return springComponentModel;
  }

  public ObjectTypeVisitor retrieveTypeVisitor() {
    return typeVisitorRetriever.get();
  }

  protected ComponentAst resolveOwnerComponent() {
    for (int i = getComponentModelHierarchy().size() - 1; i >= 0; --i) {
      final ComponentAst possibleOwner = getComponentModelHierarchy().get(i);

      if (possibleOwner.getModel(ParameterizedModel.class).isPresent()) {
        return possibleOwner;
      }
    }

    return null;
  }
}
