/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.model.dsl.config;

import static java.util.Optional.ofNullable;

import org.mule.runtime.properties.api.ConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ConfigurationProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@link ConfigurationPropertiesProvider} implementation that makes the environment variables available as configuration
 * property.
 *
 * @since 4.1
 */
public class EnvironmentPropertiesConfigurationProvider implements ConfigurationPropertiesProvider {

  private final Map<String, ConfigurationProperty> configurationAttributes = new HashMap<>();

  /**
   * Creates an {@link EnvironmentPropertiesConfigurationProvider} with the default configuration.
   */
  public EnvironmentPropertiesConfigurationProvider() {
    this(System::getenv);
  }

  /**
   * Used for testing only.
   *
   * @param environmentVariablesSupplier supplier for environment variables.
   */
  EnvironmentPropertiesConfigurationProvider(Supplier<Map<String, String>> environmentVariablesSupplier) {
    Map<String, String> environmentVariables = environmentVariablesSupplier.get();

    environmentVariables.entrySet()
        .forEach((entry) -> configurationAttributes.put(entry.getKey(),
                                                        new DefaultConfigurationProperty("environment variable", entry.getKey(),
                                                                                         entry.getValue())));
  }

  @Override
  public Optional<ConfigurationProperty> provide(String configurationAttributeKey) {
    return ofNullable(configurationAttributes.get(configurationAttributeKey));
  }

  @Override
  public String getDescription() {
    return "environment properties provider";
  }
}
