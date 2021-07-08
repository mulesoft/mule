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

class CreateParamBeanDefinitionRequest extends CreateBeanDefinitionRequest {

  private final Collection<SpringComponentModel> paramsModels;
  private final ComponentAst paramOwnerComponent;
  private final ComponentParameterAst param;

  public CreateParamBeanDefinitionRequest(List<ComponentAst> componentHierarchy,
                                          Collection<SpringComponentModel> paramsModels,
                                          ComponentAst paramOwnerComponent,
                                          ComponentParameterAst param,
                                          ComponentBuildingDefinition<?> componentBuildingDefinition,
                                          ComponentIdentifier paramComponentIdentifier) {
    super(componentHierarchy, componentBuildingDefinition, paramComponentIdentifier);

    this.paramsModels = paramsModels;
    this.paramOwnerComponent = paramOwnerComponent;
    this.param = param;
  }

  public Collection<SpringComponentModel> getParamsModels() {
    return paramsModels;
  }

  public ComponentAst getParamOwnerComponent() {
    return paramOwnerComponent;
  }

  public ComponentParameterAst getParam() {
    return param;
  }

  @Override
  public ComponentAst resolveConfigurationComponent() {
    if (getParam() != null && getParam().getValue().getRight() instanceof ComponentAst) {
      return ((ComponentAst) getParam().getValue().getRight());
    } else {
      return null;
    }
  }
}
