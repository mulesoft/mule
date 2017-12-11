/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import static java.lang.System.getProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;

/**
 * {@link ConfigurationPropertiesProvider} implementation that makes the system properties and the environment variables available
 * as configuration property.
 * <p/>
 * System properties take precedence over environment variables.
 *
 * @since 4.1
 */
public class EnvironmentPropertiesConfigurationProvider implements ConfigurationPropertiesProvider {

  private Map<String, ConfigurationProperty> configurationAttributes = new HashMap<>();

  /**
   * Creates an {@link EnvironmentPropertiesConfigurationProvider} with the default configuration.
   */
  public EnvironmentPropertiesConfigurationProvider() {
    this(System::getenv);
  }

  /**
   * Used for testing only.
   * 
   * @param environmentVariablesSupplier supplier for environment varialbes.
   */
  EnvironmentPropertiesConfigurationProvider(Supplier<Map<String, String>> environmentVariablesSupplier) {
    Map<String, String> environmentVariables = environmentVariablesSupplier.get();

    environmentVariables.entrySet().forEach((entry) -> {
      configurationAttributes.put(entry.getKey(),
                                  new ConfigurationProperty("environment variable", entry.getKey(), entry.getValue()));
    });

    Properties properties = getProperties();
    Set<Object> keys = properties.keySet();
    keys.stream().forEach(key -> {
      Object value = properties.get(key);
      if (value != null) {
        String stringKey = key instanceof String ? (String) key : key.toString();
        String stringValue = value instanceof String ? (String) value : value.toString();
        configurationAttributes.put(stringKey, new ConfigurationProperty("system property", stringKey, stringValue));
      }
    });
  }

  @Override
  public Optional<ConfigurationProperty> getConfigurationProperty(String configurationAttributeKey) {
    return Optional.ofNullable(configurationAttributes.get(configurationAttributeKey));
  }

  @Override
  public String getDescription() {
    return "environment properties provider";
  }
}
