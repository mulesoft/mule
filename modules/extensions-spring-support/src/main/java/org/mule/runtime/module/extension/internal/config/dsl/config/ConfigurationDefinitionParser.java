/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.config;

import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.config.spring.dsl.api.TypeDefinition.fromType;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getConnectedComponents;
import static org.mule.runtime.config.spring.dsl.api.xml.NameUtils.hyphenize;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition.Builder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.connection.ConnectionProviderResolver;
import org.mule.runtime.module.extension.internal.runtime.DynamicConfigPolicy;

/**
 * A {@link ExtensionDefinitionParser} for parsing {@link ConfigurationProvider}
 * instances through a {@link ConfigurationProviderObjectFactory}
 *
 * @since 4.0
 */
public final class ConfigurationDefinitionParser extends ExtensionDefinitionParser
{

    private final RuntimeConfigurationModel configurationModel;
    private final MuleContext muleContext;

    public ConfigurationDefinitionParser(Builder definition, RuntimeConfigurationModel configurationModel, MuleContext muleContext)
    {
        super(definition);
        this.configurationModel = configurationModel;
        this.muleContext = muleContext;
    }

    @Override
    protected void doParse(Builder definitionBuilder) throws ConfigurationException
    {
        definitionBuilder.withIdentifier(hyphenize(configurationModel.getName()))
                .withTypeDefinition(fromType(ConfigurationProvider.class))
                .withObjectFactoryType(ConfigurationProviderObjectFactory.class)
                .withConstructorParameterDefinition(fromSimpleParameter("name").build())
                .withConstructorParameterDefinition(fromFixedValue(configurationModel).build())
                .withConstructorParameterDefinition(fromFixedValue(muleContext).build())
                .withSetterParameterDefinition("dynamicConfigPolicy", fromChildConfiguration(DynamicConfigPolicy.class).build());

        parseParameters(configurationModel.getParameterModels());
        parseConnectionProvider(definitionBuilder);

    }

    private void parseConnectionProvider(Builder definitionBuilder)
    {
        if (!getConnectedComponents(configurationModel).isEmpty())
        {
            definitionBuilder.withSetterParameterDefinition("requiresConnection", fromFixedValue(true).build());
            definitionBuilder.withSetterParameterDefinition("connectionProviderResolver", fromChildConfiguration(ConnectionProviderResolver.class).build());
        }
    }

}
