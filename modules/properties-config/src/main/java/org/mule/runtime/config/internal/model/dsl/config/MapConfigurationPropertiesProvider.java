/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.model.dsl.config;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.properties.api.ConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ConfigurationProperty;

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
   * @param properties  map with properties to use.
   * @param description the description of the provider.
   */
  public MapConfigurationPropertiesProvider(Map<String, String> properties, String description) {
    this.properties = properties;
    this.description = description;
  }

  @Override
  public Optional<ConfigurationProperty> provide(String configurationAttributeKey) {
    String value = properties.get(configurationAttributeKey);
    if (value == null) {
      return empty();
    }
    return of(new DefaultConfigurationProperty(this, configurationAttributeKey, value));
  }

  @Override
  public String getDescription() {
    return description;
  }
}
