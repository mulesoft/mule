/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.config;

import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getConnectedComponents;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingContext;
import org.mule.runtime.module.extension.internal.runtime.DynamicConfigPolicy;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;

/**
 * A {@link ExtensionDefinitionParser} for parsing {@link ConfigurationProvider} instances through a
 * {@link ConfigurationProviderObjectFactory}
 *
 * @since 4.0
 */
public final class ConfigurationDefinitionParser extends ExtensionDefinitionParser {

  private final ExtensionModel extensionModel;
  private final ConfigurationModel configurationModel;
  private final DslElementSyntax configDsl;

  public ConfigurationDefinitionParser(ComponentBuildingDefinition.Builder definition, ExtensionModel extensionModel,
                                       ConfigurationModel configurationModel,
                                       DslSyntaxResolver dslResolver,
                                       ExtensionParsingContext parsingContext) {
    super(definition, dslResolver, parsingContext);
    this.extensionModel = extensionModel;
    this.configurationModel = configurationModel;
    this.configDsl = dslResolver.resolve(configurationModel);
  }

  @Override
  protected void doParse(ComponentBuildingDefinition.Builder definitionBuilder) throws ConfigurationException {
    definitionBuilder.withIdentifier(configDsl.getElementName()).withTypeDefinition(fromType(ConfigurationProvider.class))
        .withObjectFactoryType(ConfigurationProviderObjectFactory.class)
        .withConstructorParameterDefinition(fromSimpleParameter("name").build())
        .withConstructorParameterDefinition(fromFixedValue(extensionModel).build())
        .withConstructorParameterDefinition(fromFixedValue(configurationModel).build())
        .withConstructorParameterDefinition(fromReferenceObject(MuleContext.class).build())
        .withSetterParameterDefinition("dynamicConfigPolicy", fromChildConfiguration(DynamicConfigPolicy.class).build());

    parseParameters(configurationModel);
    parseConnectionProvider(definitionBuilder);
  }

  private void parseConnectionProvider(ComponentBuildingDefinition.Builder definitionBuilder) {
    if (!getConnectedComponents(extensionModel, configurationModel).isEmpty()) {
      definitionBuilder.withSetterParameterDefinition("requiresConnection", fromFixedValue(true).build());
      definitionBuilder.withSetterParameterDefinition("connectionProviderResolver",
                                                      fromChildConfiguration(ConnectionProviderResolver.class).build());
    }
  }

}
