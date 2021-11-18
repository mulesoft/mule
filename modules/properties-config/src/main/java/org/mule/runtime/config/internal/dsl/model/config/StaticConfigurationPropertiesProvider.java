/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import static java.util.Optional.of;

import org.mule.runtime.properties.api.ConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ConfigurationProperty;
import org.mule.runtime.properties.internal.DefaultConfigurationProperty;

import java.util.Map;
import java.util.Optional;

/**
 * Resolver for statically provided properties, not resolved form the app itself.
 *
 * @since 4.4
 */
public final class StaticConfigurationPropertiesProvider implements ConfigurationPropertiesProvider {

  // this is static to avoid reading and processing the environment properties every time
  private final ConfigurationPropertiesProvider sysPropsProvider = new SystemPropertiesConfigurationProvider();
  private final ConfigurationPropertiesProvider environmentProvider = new EnvironmentPropertiesConfigurationProvider();

  private final Map<String, String> artifactProperties;

  public StaticConfigurationPropertiesProvider(Map<String, String> artifactProperties) {
    this.artifactProperties = artifactProperties;
  }

  @Override
  public Optional<? extends ConfigurationProperty> provide(String configurationAttributeKey) {
    String propertyValue = artifactProperties.get(configurationAttributeKey);
    if (propertyValue != null) {
      return of(new DefaultConfigurationProperty(this, configurationAttributeKey, propertyValue));
    }

    Optional<? extends ConfigurationProperty> sysPropValue = sysPropsProvider.provide(configurationAttributeKey);
    if (sysPropValue.isPresent()) {
      return sysPropValue;
    }

    return environmentProvider.provide(configurationAttributeKey);
  }

  @Override
  public String getDescription() {
    return "Deployment properties";
  }
}
