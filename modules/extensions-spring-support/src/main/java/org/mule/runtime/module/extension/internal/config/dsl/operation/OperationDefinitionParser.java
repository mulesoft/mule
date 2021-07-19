/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.operation;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.ERROR_MAPPINGS;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.OUTPUT;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_NAME;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.internal.exception.EnrichedErrorMapping;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.AbstractComponentDefinitionParser;
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
  protected Builder doParse(Builder definitionBuilder) throws ConfigurationException {
    Builder finalBuilder = super.doParse(definitionBuilder);

    if (getComponentModel().getParameterGroupModels()
        .stream()
        .anyMatch(pg -> pg.getName().equals(OUTPUT))) {
      finalBuilder = finalBuilder
          .withSetterParameterDefinition(TARGET_PARAMETER_NAME,
                                         fromSimpleParameter(TARGET_PARAMETER_NAME).build())
          .withSetterParameterDefinition(TARGET_VALUE_PARAMETER_NAME,
                                         fromSimpleParameter(TARGET_VALUE_PARAMETER_NAME).build());
    }

    if (getComponentModel().getParameterGroupModels()
        .stream()
        .anyMatch(pg -> pg.getName().equals(ERROR_MAPPINGS))) {
      finalBuilder = finalBuilder
          .withSetterParameterDefinition(ERROR_MAPPINGS_PARAMETER_NAME,
                                         fromChildCollectionConfiguration(EnrichedErrorMapping.class).build());
    }

    return finalBuilder;
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
