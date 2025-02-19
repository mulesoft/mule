/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.dsl.properties;

import org.mule.runtime.properties.api.DefaultConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.DefaultConfigurationPropertiesProviderFactory;
import org.mule.runtime.properties.api.InitialisableConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ResourceProvider;

/**
 * Builds the provider for DefaultInitialisableConfigurationPropertiesProvider.
 *
 */
public final class DefaultInitialisableConfigurationPropertiesProviderFactory
    implements DefaultConfigurationPropertiesProviderFactory {

  @Override
  public InitialisableConfigurationPropertiesProvider createProvider(String fileLocation, String encoding,
                                                                     ResourceProvider resourceProvider,
                                                                     DefaultConfigurationPropertiesProvider defaultConfigurationPropertiesProvider) {
    return new DefaultInitialisableConfigurationPropertiesProvider(fileLocation, encoding, resourceProvider,
                                                                   defaultConfigurationPropertiesProvider);
  }
}
