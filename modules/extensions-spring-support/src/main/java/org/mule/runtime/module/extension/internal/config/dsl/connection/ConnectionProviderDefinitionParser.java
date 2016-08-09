/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.connection;

import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.config.spring.dsl.api.TypeDefinition.fromType;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition.Builder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.xml.dsl.api.DslElementSyntax;
import org.mule.runtime.extension.xml.dsl.api.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;

/**
 * A {@link ExtensionDefinitionParser} for parsing {@link ConnectionProviderResolver} instances through a
 * {@link ConnectionProviderObjectFactory}
 *
 * @since 4.0
 */
public final class ConnectionProviderDefinitionParser extends ExtensionDefinitionParser {

  private final ConnectionProviderModel providerModel;
  private final MuleContext muleContext;
  private final DslElementSyntax connectionDsl;

  public ConnectionProviderDefinitionParser(Builder definition, ConnectionProviderModel providerModel,
                                            DslSyntaxResolver dslSyntaxResolver, MuleContext muleContext,
                                            ExtensionParsingContext parsingContext) {
    super(definition, dslSyntaxResolver, parsingContext);
    this.providerModel = providerModel;
    this.muleContext = muleContext;
    this.connectionDsl = dslSyntaxResolver.resolve(providerModel);
  }

  @Override
  protected void doParse(Builder definitionBuilder) throws ConfigurationException {
    definitionBuilder.withIdentifier(connectionDsl.getElementName())
        .withTypeDefinition(fromType(ConnectionProviderResolver.class))
        .withObjectFactoryType(ConnectionProviderObjectFactory.class)
        .withConstructorParameterDefinition(fromFixedValue(providerModel).build())
        .withConstructorParameterDefinition(fromFixedValue(muleContext).build())
        .withSetterParameterDefinition("disableValidation", fromSimpleParameter("disableValidation").build())
        .withSetterParameterDefinition("retryPolicyTemplate", fromChildConfiguration(RetryPolicyTemplate.class).build())
        .withSetterParameterDefinition("poolingProfile", fromChildConfiguration(PoolingProfile.class).build());

    parseParameters(providerModel.getParameterModels());
  }
}
