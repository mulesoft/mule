/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.test.policy;


import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.policy.DefaultPolicyInstance;
import org.mule.runtime.core.policy.PolicyChain;
import org.mule.runtime.core.policy.PolicyNextActionMessageProcessor;
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
    ArrayList<ComponentBuildingDefinition> definitions = new ArrayList<>();

    definitions.add(baseDefinition.copy().withIdentifier("proxy")
        .asPrototype()
        .withTypeDefinition(fromType(DefaultPolicyInstance.class))
        .withSetterParameterDefinition("sourcePolicyChain",
                                       fromChildConfiguration(PolicyChain.class).withWrapperIdentifier("source").build())
        .withSetterParameterDefinition("operationPolicyChain",
                                       fromChildConfiguration(PolicyChain.class).withWrapperIdentifier("operation").build())
        .build());

    definitions.add(baseDefinition.copy().withIdentifier("source")
        .withTypeDefinition(fromType(PolicyChain.class))
        .withSetterParameterDefinition("processors", fromChildCollectionConfiguration(Processor.class).build()).build());

    definitions.add(baseDefinition.copy().withIdentifier("operation")
        .withTypeDefinition(fromType(PolicyChain.class))
        .withSetterParameterDefinition("processors", fromChildCollectionConfiguration(Processor.class).build()).build());

    definitions.add(baseDefinition.copy().withIdentifier("execute-next")
        .withTypeDefinition(fromType(PolicyNextActionMessageProcessor.class)).build());

    return definitions;
  }
}
