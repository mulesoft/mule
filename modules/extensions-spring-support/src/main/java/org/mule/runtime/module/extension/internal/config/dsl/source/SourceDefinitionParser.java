/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.source;

import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleReferenceParameter;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingContext;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource;

import java.util.List;

/**
 * An {@link ExtensionMessageSource} used to parse instances of {@link ExtensionMessageSource} instances through a
 * {@link SourceDefinitionParser}
 *
 * @since 4.0
 */
public class SourceDefinitionParser extends ExtensionDefinitionParser {

  private final ExtensionModel extensionModel;
  private final SourceModel sourceModel;
  private final DslElementSyntax sourceDsl;

  public SourceDefinitionParser(ComponentBuildingDefinition.Builder definition, ExtensionModel extensionModel,
                                SourceModel sourceModel, DslSyntaxResolver dslSyntaxResolver, MuleContext muleContext,
                                ExtensionParsingContext parsingContext) {
    super(definition, dslSyntaxResolver, parsingContext, muleContext);
    this.extensionModel = extensionModel;
    this.sourceModel = sourceModel;
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
        .withSetterParameterDefinition(CONFIG_PROVIDER_ATTRIBUTE_NAME,
                                       fromSimpleReferenceParameter(CONFIG_ATTRIBUTE_NAME).build())
        .withSetterParameterDefinition(CURSOR_STREAM_PROVIDER_FACTORY_FIELD_NAME,
                                       fromChildConfiguration(CursorStreamProviderFactory.class).build());

    List<ParameterGroupModel> inlineGroups = getInlineGroups(sourceModel);
    sourceModel.getErrorCallback().ifPresent(cb -> inlineGroups.addAll(getInlineGroups(cb)));
    sourceModel.getSuccessCallback().ifPresent(cb -> inlineGroups.addAll(getInlineGroups(cb)));

    parseParameters(getFlatParameters(inlineGroups, sourceModel.getAllParameterModels()));

    for (ParameterGroupModel group : inlineGroups) {
      parseParameterGroup(group);
    }
  }
}
