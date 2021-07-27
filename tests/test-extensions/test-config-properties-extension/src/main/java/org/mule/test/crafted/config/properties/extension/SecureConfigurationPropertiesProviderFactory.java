/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.crafted.config.properties.extension;

import static java.util.Objects.requireNonNull;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.test.crafted.config.properties.extension.TestConfigPropertiesExtensionLoadingDelegate.EXTENSION_NAME;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.properties.api.ConfigurationPropertiesProviderFactory;
import org.mule.runtime.properties.api.ResourceProvider;

import java.util.function.UnaryOperator;

/**
 * Builds the provider for the secure-configuration-properties element.
 *
 * @since 4.1
 */
public class SecureConfigurationPropertiesProviderFactory implements ConfigurationPropertiesProviderFactory {

  public static final String SECURE_CONFIGURATION_PROPERTIES_ELEMENT = "secure-configuration-properties-config";
  public static final ComponentIdentifier SECURE_CONFIGURATION_PROPERTIES =
      builder().namespace(EXTENSION_NAME).name(SECURE_CONFIGURATION_PROPERTIES_ELEMENT).build();

  @Override
  public ComponentIdentifier getSupportedComponentIdentifier() {
    return SECURE_CONFIGURATION_PROPERTIES;
  }

  @Override
  public SecureConfigurationPropertiesProvider createProvider(ComponentAst providerElementDeclaration,
                                                              UnaryOperator<String> localResolver,
                                                              ResourceProvider externalResourceProvider) {
    String file = providerElementDeclaration.getParameter(DEFAULT_GROUP_NAME, "file").getResolvedRawValue();
    requireNonNull(file, "Required attribute 'file' of 'secure-configuration-properties' not found");

    String algorithm = providerElementDeclaration.getParameter("encrypt", "algorithm").getResolvedRawValue();
    String mode = providerElementDeclaration.getParameter("encrypt", "mode").getResolvedRawValue();

    return new SecureConfigurationPropertiesProvider(externalResourceProvider, file, algorithm, mode);
  }

}
