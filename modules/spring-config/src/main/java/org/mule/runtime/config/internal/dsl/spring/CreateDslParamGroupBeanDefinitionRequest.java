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
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

import java.util.Collection;
import java.util.List;

public class CreateDslParamGroupBeanDefinitionRequest extends CreateBeanDefinitionRequest {

  public CreateDslParamGroupBeanDefinitionRequest(List<ComponentAst> componentHierarchy,
                                                  Collection<SpringComponentModel> paramsModels,
                                                  ComponentAst paramOwnerComponent,
                                                  ComponentBuildingDefinition<?> componentBuildingDefinition,
                                                  ComponentIdentifier paramComponentIdentifier) {
    super(componentHierarchy, null, paramsModels, paramOwnerComponent, null, componentBuildingDefinition,
          paramComponentIdentifier);
  }

  @Override
  public ComponentAst resolveConfigurationComponent() {
    return null;
  }
}
