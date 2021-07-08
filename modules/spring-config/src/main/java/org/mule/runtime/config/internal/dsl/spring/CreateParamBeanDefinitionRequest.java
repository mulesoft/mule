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

public class CreateParamBeanDefinitionRequest implements CreateBeanDefinitionRequest {

  private final List<ComponentAst> componentModelHierarchy;
  private final Collection<SpringComponentModel> paramsModels;
  private final ComponentAst paramOwnerComponentModel;
  private final ComponentParameterAst param;
  private final ComponentBuildingDefinition componentBuildingDefinition;
  private final SpringComponentModel springComponentModel;

  /**
   * @param parentComponentModel        the container element of the holder for the configuration attributes defined by the user
   * @param componentModel              the holder for the configuration attributes defined by the user
   * @param componentBuildingDefinition the definition to build the domain object that will represent the configuration on runtime
   */
  public CreateParamBeanDefinitionRequest(List<ComponentAst> componentModelHierarchy,
                                          Collection<SpringComponentModel> paramsModels,
                                          ComponentAst paramOwnerComponentModel,
                                          ComponentParameterAst param,
                                          ComponentBuildingDefinition componentBuildingDefinition,
                                          ComponentIdentifier componentIdentifier) {
    this.componentModelHierarchy = componentModelHierarchy;
    this.paramsModels = paramsModels;
    this.paramOwnerComponentModel = paramOwnerComponentModel;
    this.param = param;
    this.componentBuildingDefinition = componentBuildingDefinition;
    this.springComponentModel = new SpringComponentModel();
    springComponentModel.setComponentIdentifier(componentIdentifier);

    ObjectTypeVisitor typeVisitor = new ObjectTypeVisitor(null);

    if (componentBuildingDefinition != null) {
      componentBuildingDefinition.getTypeDefinition().visit(typeVisitor);
    } else {
      typeVisitor.onType(Object.class);
    }

    springComponentModel.setType(typeVisitor.getType());
    typeVisitor.getMapEntryType().ifPresent(met -> springComponentModel.setMapEntryType(met));
  }

  @Override
  public SpringComponentModel getSpringComponentModel() {
    return springComponentModel;
  }

  @Override
  public ComponentBuildingDefinition getComponentBuildingDefinition() {
    return componentBuildingDefinition;
  }

  @Override
  public Collection<SpringComponentModel> getParamsModels() {
    return paramsModels;
  }

  public ComponentParameterAst getParam() {
    return param;
  }

  public List<ComponentAst> getComponentModelHierarchy() {
    return componentModelHierarchy;
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
