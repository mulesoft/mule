/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.source;

import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.config.spring.dsl.api.TypeDefinition.fromType;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.CONFIG_ATTRIBUTE;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.source.RuntimeSourceModel;
import org.mule.runtime.extension.xml.dsl.api.DslElementSyntax;
import org.mule.runtime.extension.xml.dsl.api.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingContext;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource;

/**
 * An {@link ExtensionMessageSource} used to parse instances of {@link ExtensionMessageSource} instances through a
 * {@link SourceDefinitionParser}
 *
 * @since 4.0
 */
public class SourceDefinitionParser extends ExtensionDefinitionParser {

  private final RuntimeExtensionModel extensionModel;
  private final RuntimeSourceModel sourceModel;
  private final MuleContext muleContext;
  private final DslElementSyntax sourceDsl;

  public SourceDefinitionParser(ComponentBuildingDefinition.Builder definition, RuntimeExtensionModel extensionModel,
                                RuntimeSourceModel sourceModel, DslSyntaxResolver dslSyntaxResolver, MuleContext muleContext,
                                ExtensionParsingContext parsingContext) {
    super(definition, dslSyntaxResolver, parsingContext);
    this.extensionModel = extensionModel;
    this.sourceModel = sourceModel;
    this.muleContext = muleContext;
    this.sourceDsl = dslSyntaxResolver.resolve(sourceModel);
  }

  @Override
  protected void doParse(ComponentBuildingDefinition.Builder definitionBuilder) throws ConfigurationException {
    definitionBuilder.withIdentifier(sourceDsl.getElementName()).withTypeDefinition(fromType(ExtensionMessageSource.class))
        .withObjectFactoryType(ExtensionSourceObjectFactory.class)
        .withConstructorParameterDefinition(fromFixedValue(extensionModel).build())
        .withConstructorParameterDefinition(fromFixedValue(sourceModel).build())
        .withConstructorParameterDefinition(fromFixedValue(muleContext).build())
        .withSetterParameterDefinition("retryPolicyTemplate", fromChildConfiguration(RetryPolicyTemplate.class).build())
        .withSetterParameterDefinition("configurationProviderName", fromSimpleParameter(CONFIG_ATTRIBUTE).build());

    parseParameters(sourceModel.getParameterModels());
  }
}
