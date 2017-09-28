/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.util.Map;
import java.util.Optional;

/**
 * {@link ConfigurationPropertiesProvider} wrapper for properties.
 *
 * @since 4.0
 */
public class MapConfigurationPropertiesProvider implements ConfigurationPropertiesProvider {

  private final Map<String, String> properties;
  private final String description;

  /**
   * Creates a new instance
   *
   * @param properties map with properties to use.
   * @param description the description of the provider.
   */
  public MapConfigurationPropertiesProvider(Map<String, String> properties, String description) {
    this.properties = properties;
    this.description = description;
  }

  @Override
  public Optional<ConfigurationProperty> getConfigurationProperty(String configurationAttributeKey) {
    String value = properties.get(configurationAttributeKey);
    if (value == null) {
      return empty();
    }
    return of(new ConfigurationProperty(this, configurationAttributeKey, value));
  }

  @Override
  public String getDescription() {
    return description;
  }
}
