/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.operation;

import org.mule.metadata.api.model.VoidType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.AbstractComponentDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingContext;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;

import java.util.concurrent.atomic.AtomicBoolean;

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
    return true;
  }

  @Override
  protected boolean hasOutputGroup() {
    AtomicBoolean isVoid = new AtomicBoolean();
    getComponentModel().getOutput().getType().accept(new MetadataTypeVisitor() {

      @Override
      public void visitVoid(VoidType voidType) {
        isVoid.set(true);
      }
    });

    return !isVoid.get();
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
