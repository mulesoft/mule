/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.properties;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.config.api.dsl.model.ConfigurationParameters;
import org.mule.runtime.config.api.dsl.model.ResourceProvider;

/**
 * Builds the provider for the configuration-properties element.
 *
 * @since 4.1
 */
public class DefaultConfigurationPropertiesProviderFactory implements ConfigurationPropertiesProviderFactory {

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

    return new DefaultConfigurationPropertiesProvider(file, externalResourceProvider);
  }
}
