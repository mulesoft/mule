/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.operation;

import static org.mule.runtime.extension.privileged.util.ComponentDeclarationUtils.isNoErrorMapping;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.VoidType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingContext;
import org.mule.runtime.module.extension.internal.parser.AbstractComponentDefinitionParser;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;

import java.util.Optional;

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
                                   ExtensionParsingContext parsingContext,
                                   Optional<ClassTypeLoader> typeLoader) {
    super(definition, extensionModel, operationModel, dslSyntaxResolver, parsingContext, typeLoader);
  }

  @Override
  protected boolean hasErrorMappingsGroup() {
    return !isNoErrorMapping(getComponentModel());
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
