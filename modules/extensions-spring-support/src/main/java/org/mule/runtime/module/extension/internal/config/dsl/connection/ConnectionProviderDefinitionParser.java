/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.connection;

import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingContext;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthManager;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;

/**
 * A {@link ExtensionDefinitionParser} for parsing {@link ConnectionProviderResolver} instances through a
 * {@link ConnectionProviderObjectFactory}
 *
 * @since 4.0
 */
public final class ConnectionProviderDefinitionParser extends ExtensionDefinitionParser {

  private final ConnectionProviderModel providerModel;
  private final ExtensionModel extensionModel;
  private final DslElementSyntax connectionDsl;

  public ConnectionProviderDefinitionParser(Builder definition, ConnectionProviderModel providerModel,
                                            ExtensionModel extensionModel, DslSyntaxResolver dslSyntaxResolver,
                                            ExtensionParsingContext parsingContext) {
    super(definition, dslSyntaxResolver, parsingContext);
    this.providerModel = providerModel;
    this.extensionModel = extensionModel;
    this.connectionDsl = dslSyntaxResolver.resolve(providerModel);
  }

  @Override
  protected Builder doParse(Builder definitionBuilder) throws ConfigurationException {
    Builder finalBuilder = definitionBuilder.withIdentifier(connectionDsl.getElementName())
        .withTypeDefinition(fromType(ConnectionProviderResolver.class))
        .withObjectFactoryType(ConnectionProviderObjectFactory.class)
        .withConstructorParameterDefinition(fromFixedValue(providerModel).build())
        .withConstructorParameterDefinition(fromFixedValue(extensionModel).build())
        .withConstructorParameterDefinition(fromReferenceObject(ExtensionsOAuthManager.class).build())
        .withConstructorParameterDefinition(fromReferenceObject(MuleContext.class).build())
        .withSetterParameterDefinition("reconnectionConfig",
                                       fromChildConfiguration(ReconnectionConfig.class).build())
        .withSetterParameterDefinition("poolingProfile", fromChildConfiguration(PoolingProfile.class).build());

    parseParameters(providerModel);

    return finalBuilder;
  }
}
