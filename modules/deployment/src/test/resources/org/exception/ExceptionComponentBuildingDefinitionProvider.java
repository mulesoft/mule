/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.exception;

import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleReferenceParameter;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import org.mule.functional.api.component.ThrowProcessor;
import org.mule.runtime.ast.internal.error.DefaultErrorTypeRepository;
import org.mule.runtime.ast.internal.error.ErrorTypeBuilder;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;

import java.util.ArrayList;
import java.util.List;


public class ExceptionComponentBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider {

  private ComponentBuildingDefinition.Builder baseDefinition;

  @Override
  public void init() {
    baseDefinition = new ComponentBuildingDefinition.Builder().withNamespace("ex");
  }

  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
    List<ComponentBuildingDefinition> componentBuildingDefinitions = new ArrayList<>();


    componentBuildingDefinitions.add(baseDefinition
                                             .withIdentifier("throw-exception")
                                             .withTypeDefinition(fromType(ThrowProcessor.class))
                                             .withSetterParameterDefinition("exception", fromSimpleParameter("exceptionClassName").build())
                                             .withSetterParameterDefinition("error", fromSimpleParameter("error").build())
                                             .withSetterParameterDefinition("count", fromSimpleParameter("count").withDefaultValue(-1).build()).build());
    return componentBuildingDefinitions;
  }

}

