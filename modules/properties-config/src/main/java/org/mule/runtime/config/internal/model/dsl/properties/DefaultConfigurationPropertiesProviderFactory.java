/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.dsl.properties;

import static java.util.Objects.requireNonNull;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.properties.api.ConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ConfigurationPropertiesProviderFactory;
import org.mule.runtime.properties.api.ResourceProvider;

import java.util.function.UnaryOperator;

/**
 * Builds the provider for the configuration-properties element.
 *
 * @since 4.4
 */
public final class DefaultConfigurationPropertiesProviderFactory implements ConfigurationPropertiesProviderFactory {

  public static final String CONFIGURATION_PROPERTIES_ELEMENT = "configuration-properties";
  private static final String CORE_PREFIX = "mule";
  public static final ComponentIdentifier CONFIGURATION_PROPERTIES =
      builder().namespace(CORE_PREFIX).name(CONFIGURATION_PROPERTIES_ELEMENT).build();

  @Override
  public ComponentIdentifier getSupportedComponentIdentifier() {
    return CONFIGURATION_PROPERTIES;
  }

  @Override
  public ConfigurationPropertiesProvider createProvider(ComponentAst providerElementDeclaration,
                                                        UnaryOperator<String> localResolver,
                                                        ResourceProvider externalResourceProvider) {
    String file = requireNonNull(providerElementDeclaration.getParameter(DEFAULT_GROUP_NAME, "file").getResolvedRawValue(),
                                 "Required attribute 'file' of 'configuration-properties' not found");
    final ComponentParameterAst encodingParam = providerElementDeclaration.getParameter(DEFAULT_GROUP_NAME, "encoding");
    String encoding = encodingParam != null ? encodingParam.getResolvedRawValue() : null;
    return new DefaultConfigurationPropertiesProvider(file, encoding, externalResourceProvider);
  }
}
