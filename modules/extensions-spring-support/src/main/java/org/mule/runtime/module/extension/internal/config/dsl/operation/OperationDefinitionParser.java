/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.operation;

import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleReferenceParameter;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.policy.PolicyManager;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingContext;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;

import java.util.List;

/**
 * A {@link ExtensionDefinitionParser} for parsing {@link OperationMessageProcessor} instances through a
 * {@link OperationMessageProcessorObjectFactory}
 *
 * @since 4.0
 */
public class OperationDefinitionParser extends ExtensionDefinitionParser {

  private final ExtensionModel extensionModel;
  private final OperationModel operationModel;
  private final DslElementSyntax operationDsl;

  public OperationDefinitionParser(Builder definition, ExtensionModel extensionModel,
                                   OperationModel operationModel,
                                   DslSyntaxResolver dslSyntaxResolver, MuleContext muleContext,
                                   ExtensionParsingContext parsingContext) {
    super(definition, dslSyntaxResolver, parsingContext, muleContext);
    this.extensionModel = extensionModel;
    this.operationModel = operationModel;
    this.operationDsl = dslSyntaxResolver.resolve(operationModel);
  }

  @Override
  protected void doParse(Builder definitionBuilder) throws ConfigurationException {
    definitionBuilder.withIdentifier(operationDsl.getElementName()).withTypeDefinition(fromType(OperationMessageProcessor.class))
        .withObjectFactoryType(OperationMessageProcessorObjectFactory.class)
        .withConstructorParameterDefinition(fromFixedValue(extensionModel).build())
        .withConstructorParameterDefinition(fromFixedValue(operationModel).build())
        .withConstructorParameterDefinition(fromReferenceObject(MuleContext.class).build())
        .withConstructorParameterDefinition(fromReferenceObject(PolicyManager.class).build())
        .withSetterParameterDefinition(TARGET_PARAMETER_NAME, fromSimpleParameter(TARGET_PARAMETER_NAME).build())
        .withSetterParameterDefinition(CONFIG_PROVIDER_ATTRIBUTE_NAME,
                                       fromSimpleReferenceParameter(CONFIG_ATTRIBUTE_NAME).build())
        .withSetterParameterDefinition(CURSOR_STREAM_PROVIDER_FACTORY_FIELD_NAME,
                                       fromChildConfiguration(CursorStreamProviderFactory.class).build());

    List<ParameterGroupModel> inlineGroups = getInlineGroups(operationModel);

    parseParameters(getFlatParameters(inlineGroups, operationModel.getAllParameterModels()));

    for (ParameterGroupModel group : inlineGroups) {
      parseParameterGroup(group);
    }
  }
}
