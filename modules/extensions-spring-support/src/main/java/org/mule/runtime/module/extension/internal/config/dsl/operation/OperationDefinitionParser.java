/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.config.dsl.operation;

import org.mule.metadata.api.model.VoidType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.internal.property.NoErrorMappingModelProperty;
import org.mule.runtime.module.extension.internal.parser.AbstractComponentDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingContext;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;

/**
 * A {@link ExtensionDefinitionParser} for parsing {@link OperationMessageProcessor} instances through a
 * {@link OperationMessageProcessorObjectFactory}
 *
 * @since 4.0
 */
public class OperationDefinitionParser extends AbstractComponentDefinitionParser<OperationModel> {

  public OperationDefinitionParser(Builder definition, ExtensionModel extensionModel,
                                   OperationModel operationModel,
                                   DslSyntaxResolver dslSyntaxResolver,
                                   ExtensionParsingContext parsingContext) {
    super(definition, extensionModel, operationModel, dslSyntaxResolver, parsingContext);
  }

  @Override
  protected boolean hasErrorMappingsGroup() {
    return !getComponentModel().getModelProperty(NoErrorMappingModelProperty.class).isPresent();
  }

  @Override
  protected boolean hasOutputGroup() {
    return !(getComponentModel().getOutput().getType() instanceof VoidType);
  }

  @Override
  protected Class<OperationMessageProcessor> getMessageProcessorType() {
    return OperationMessageProcessor.class;
  }

  @Override
  protected Class<OperationMessageProcessorObjectFactory> getMessageProcessorFactoryType() {
    return OperationMessageProcessorObjectFactory.class;
  }
}
