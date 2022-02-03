/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.config.provider;

import static java.util.Arrays.asList;
import static org.mule.runtime.config.internal.dsl.processor.xml.OperationDslNamespaceInfoProvider.OPERATION_DSL_NAMESPACE;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Operation;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.module.extension.mule.internal.config.factory.DefaultOperationFactoryBean;

import java.util.List;

public class OperationDslBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider {

  private static ComponentBuildingDefinition.Builder baseDefinition =
      new ComponentBuildingDefinition.Builder().withNamespace(OPERATION_DSL_NAMESPACE);

  @Override
  public void init() {

  }

  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
    return asList(
        baseDefinition.withIdentifier("def")
            .withTypeDefinition(fromType(Operation.class))
            .withObjectFactoryType(DefaultOperationFactoryBean.class)
            .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
            .withSetterParameterDefinition("body", fromChildConfiguration(OperationBody.class).build())
            .withSetterParameterDefinition("extensionManager", fromReferenceObject(ExtensionManager.class).build())
            .withSetterParameterDefinition("muleContext", fromReferenceObject(MuleContext.class).build())
            .build(),
        baseDefinition.withIdentifier("body")
            .withTypeDefinition(fromType(OperationBody.class))
            .withSetterParameterDefinition("processors", fromChildCollectionConfiguration(Processor.class).build())
            .build()
    );
  }

  public static class OperationBody {

    private List<Processor> processors;

    public List<Processor> getProcessors() {
      return processors;
    }

    public void setProcessors(List<Processor> processors) {
      this.processors = processors;
    }
  }
}
