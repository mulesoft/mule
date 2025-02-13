/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class CreateParamBeanDefinitionRequest extends CreateBeanDefinitionRequest {

  private final ComponentAst paramOwnerComponent;
  private final ComponentParameterAst param;
  private final Consumer<ComponentAst> nestedComponentParamProcessor;

  public CreateParamBeanDefinitionRequest(List<ComponentAst> componentHierarchy,
                                          Collection<SpringComponentModel> paramsModels,
                                          ComponentAst paramOwnerComponent,
                                          ComponentParameterAst param,
                                          ComponentBuildingDefinition<?> componentBuildingDefinition,
                                          ComponentIdentifier paramComponentIdentifier,
                                          Consumer<ComponentAst> nestedComponentParamProcessor) {
    super(componentHierarchy, null, paramsModels, componentBuildingDefinition, paramComponentIdentifier);
    this.paramOwnerComponent = paramOwnerComponent;
    this.param = param;
    this.nestedComponentParamProcessor = nestedComponentParamProcessor;
  }

  @Override
  public ComponentAst resolveConfigurationComponent() {
    if (getParam().getValue().getRight() instanceof ComponentAst) {
      return ((ComponentAst) getParam().getValue().getRight());
    } else {
      return null;
    }
  }

  public ComponentAst getParamOwnerComponent() {
    return paramOwnerComponent;
  }

  public ComponentParameterAst getParam() {
    return param;
  }

  public Consumer<ComponentAst> getNestedComponentParamProcessor() {
    return nestedComponentParamProcessor;
  }

  @Override
  public ComponentParameterAst getParameter(String parameterName) {
    // TODO MULE-19672 When decoupling from the dsl representation, properly propagate the group information to use here instead
    // of iterating.
    return resolveOwnerComponent().getParameters().stream()
        .filter(p -> p.getModel().getName().equals(parameterName))
        .findFirst()
        .orElse(null);
  }
}
