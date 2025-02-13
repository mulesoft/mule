/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.properties;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.CORE_PREFIX;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.ConfigurationParameters;
import org.mule.runtime.properties.api.ConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ResourceProvider;
import org.mule.runtime.config.api.properties.DefaultConfigurationPropertiesProvider;

import java.util.function.UnaryOperator;

/**
 * Builds the provider for the configuration-properties element.
 *
 * @since 4.1
 * @deprecated since 4.4, use the SDK instead of registering parsers manually.
 */
@Deprecated
public final class DefaultConfigurationPropertiesProviderFactory implements ConfigurationPropertiesProviderFactory {

  public static final String CONFIGURATION_PROPERTIES_ELEMENT = "configuration-properties";
  public static final ComponentIdentifier CONFIGURATION_PROPERTIES =
      builder().namespace(CORE_PREFIX).name(CONFIGURATION_PROPERTIES_ELEMENT).build();

  @Override
  public ComponentIdentifier getSupportedComponentIdentifier() {
    return CONFIGURATION_PROPERTIES;
  }

  @Override
  public ConfigurationPropertiesProvider createProvider(ConfigurationParameters parameters,
                                                        ResourceProvider externalResourceProvider) {

    String file = parameters.getStringParameter("file");
    Preconditions.checkArgument(file != null, "Required attribute 'file' of 'configuration-properties' not found");
    String encoding = parameters.getStringParameter("encoding");
    return new DefaultConfigurationPropertiesProvider(file, encoding, externalResourceProvider);
  }

  @Override
  public ConfigurationPropertiesProvider createProvider(ComponentAst providerElementDeclaration,
                                                        UnaryOperator<String> localResolver,
                                                        ResourceProvider externalResourceProvider) {
    return createProvider(providerElementDeclaration, localResolver, uri -> externalResourceProvider.getResourceAsStream(uri));
  }

  @Override
  public org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProvider createProvider(ConfigurationParameters parameters,
                                                                                                         org.mule.runtime.config.api.dsl.model.ResourceProvider externalResourceProvider) {
    return createProvider(parameters, uri -> externalResourceProvider.getResourceAsStream(uri));
  }
}
