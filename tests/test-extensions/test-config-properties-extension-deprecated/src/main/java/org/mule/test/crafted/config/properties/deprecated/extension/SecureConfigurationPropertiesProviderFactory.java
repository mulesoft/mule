/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.crafted.config.properties.deprecated.extension;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.internal.dsl.DslConstants.EE_PREFIX;
import static org.mule.test.crafted.config.properties.deprecated.extension.TestConfigPropertiesExtensionLoadingDelegate.EXTENSION_NAME;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.config.api.dsl.model.ConfigurationParameters;
import org.mule.runtime.config.api.dsl.model.ResourceProvider;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProviderFactory;

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
  public SecureConfigurationPropertiesProvider createProvider(ConfigurationParameters parameters,
                                                              ResourceProvider externalResourceProvider) {
    String file = parameters.getStringParameter("file");
    Preconditions.checkArgument(file != null, "Required attribute 'file' of 'secure-configuration-properties' not found");

    ComponentIdentifier encryptComponentIdentifier =
        ComponentIdentifier.builder().namespace(EXTENSION_NAME).name("encrypt").build();
    String algorithm =
        parameters.getComplexConfigurationParameter(encryptComponentIdentifier).get(0).getStringParameter("algorithm");
    String mode = parameters.getComplexConfigurationParameter(encryptComponentIdentifier).get(0).getStringParameter("mode");

    return new SecureConfigurationPropertiesProvider(externalResourceProvider, file, algorithm, mode);
  }

}
