/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.operation;

import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.config.spring.dsl.api.TypeDefinition.fromType;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TARGET_ATTRIBUTE;
import static org.mule.runtime.config.spring.dsl.api.xml.NameUtils.hyphenize;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.CONFIG_ATTRIBUTE;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition.Builder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.operation.RuntimeOperationModel;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser;
import org.mule.runtime.module.extension.internal.runtime.processor.OperationMessageProcessor;

/**
 * A {@link ExtensionDefinitionParser} for parsing {@link OperationMessageProcessor}
 * instances through a {@link OperationMessageProcessorObjectFactory}
 *
 * @since 4.0
 */
public class OperationDefinitionParser extends ExtensionDefinitionParser
{

    private final RuntimeExtensionModel extensionModel;
    private final RuntimeOperationModel operationModel;
    private final MuleContext muleContext;

    public OperationDefinitionParser(Builder definition, RuntimeExtensionModel extensionModel, RuntimeOperationModel operationModel, MuleContext muleContext)
    {
        super(definition);
        this.extensionModel = extensionModel;
        this.operationModel = operationModel;
        this.muleContext = muleContext;
    }

    @Override
    protected void doParse(Builder definitionBuilder) throws ConfigurationException
    {
        definitionBuilder.withIdentifier(hyphenize(operationModel.getName()))
                .withTypeDefinition(fromType(OperationMessageProcessor.class))
                .withObjectFactoryType(OperationMessageProcessorObjectFactory.class)
                .withConstructorParameterDefinition(fromFixedValue(extensionModel).build())
                .withConstructorParameterDefinition(fromFixedValue(operationModel).build())
                .withConstructorParameterDefinition(fromFixedValue(muleContext).build())
                .withSetterParameterDefinition(TARGET_ATTRIBUTE, fromSimpleParameter(TARGET_ATTRIBUTE).build())
                .withSetterParameterDefinition("configurationProviderName", fromSimpleParameter(CONFIG_ATTRIBUTE).build());

        parseParameters(operationModel.getParameterModels());
    }
}
