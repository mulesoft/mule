/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.dsl.spring;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

import java.util.List;
import java.util.function.Consumer;

public class CreateComponentBeanDefinitionRequest extends CreateBeanDefinitionRequest {

  private final Consumer<ComponentAst> nestedComponentParamProcessor;

  public CreateComponentBeanDefinitionRequest(List<ComponentAst> componentHierarchy,
                                              ComponentAst component,
                                              List<SpringComponentModel> paramsModels,
                                              ComponentBuildingDefinition componentBuildingDefinition,
                                              Consumer<ComponentAst> nestedComponentParamProcessor) {
    super(componentHierarchy, component, paramsModels, componentBuildingDefinition, component.getIdentifier());
    this.nestedComponentParamProcessor = nestedComponentParamProcessor;
  }

  @Override
  public ComponentAst resolveConfigurationComponent() {
    return getComponent();
  }

  public Consumer<ComponentAst> getNestedComponentParamProcessor() {
    return nestedComponentParamProcessor;
  }
}
