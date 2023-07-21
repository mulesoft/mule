/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.policy.api;

import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromConfigurationAttribute;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;

import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.policy.DefaultPolicyInstance;
import org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides a {@link ComponentBuildingDefinitionProvider} for the test policy module
 */
public class TestPolicyBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider {

  private ComponentBuildingDefinition.Builder baseDefinition;

  @Override
  public void init() {
    baseDefinition = new ComponentBuildingDefinition.Builder()
        .withNamespace("test-policy");
  }

  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
    List<ComponentBuildingDefinition> definitions = new ArrayList<>();

    definitions.add(baseDefinition.withIdentifier("proxy")
        .withTypeDefinition(fromType(DefaultPolicyInstance.class))
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("sourcePolicyChain",
                                       fromChildConfiguration(PolicyChain.class).withWrapperIdentifier("source").build())
        .withSetterParameterDefinition("operationPolicyChain",
                                       fromChildConfiguration(PolicyChain.class).withWrapperIdentifier("operation").build())
        .build());

    definitions.add(baseDefinition.withIdentifier("source")
        .withTypeDefinition(fromType(PolicyChain.class))
        .withSetterParameterDefinition("processors", fromChildCollectionConfiguration(Processor.class).build()).build());

    definitions.add(baseDefinition.withIdentifier("operation")
        .withTypeDefinition(fromType(PolicyChain.class))
        .withSetterParameterDefinition("processors", fromChildCollectionConfiguration(Processor.class).build()).build());

    definitions.add(baseDefinition.withIdentifier("execute-next")
        .withTypeDefinition(fromType(PolicyNextActionMessageProcessor.class)).build());

    definitions.add(baseDefinition.withIdentifier("custom-processor")
        .withTypeDefinition(fromConfigurationAttribute("class")).build());

    return definitions;
  }
}
