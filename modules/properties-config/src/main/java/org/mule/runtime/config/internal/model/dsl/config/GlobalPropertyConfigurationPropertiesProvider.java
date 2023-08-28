/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.model.dsl.config;

import static java.util.Optional.ofNullable;

import org.mule.runtime.properties.api.ConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ConfigurationProperty;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class GlobalPropertyConfigurationPropertiesProvider implements ConfigurationPropertiesProvider {

  private final Supplier<Map<String, ConfigurationProperty>> globalPropertiesConfigurationAttributes;

  public GlobalPropertyConfigurationPropertiesProvider(Supplier<Map<String, ConfigurationProperty>> properties) {
    this.globalPropertiesConfigurationAttributes = properties;
  }


  @Override
  public Optional<ConfigurationProperty> provide(String configurationAttributeKey) {
    return ofNullable(globalPropertiesConfigurationAttributes.get().get(configurationAttributeKey));
  }

  @Override
  public String getDescription() {
    return "global-properties configuration attributes";
  }

  @Override
  public String toString() {
    return getDescription();
  }
}
