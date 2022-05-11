/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import static java.lang.System.getProperties;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toMap;

import org.mule.runtime.properties.api.ConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ConfigurationProperty;

import java.util.Map;
import java.util.Optional;

/**
 * {@link ConfigurationPropertiesProvider} implementation that makes the system properties available as configuration property.
 * <p/>
 * System properties take precedence over environment variables.
 *
 * @since 4.5
 */
public class SystemPropertiesConfigurationProvider implements ConfigurationPropertiesProvider {

  private final Map<String, String> properties;

  public SystemPropertiesConfigurationProvider() {
    properties = getProperties().entrySet().stream().collect(toMap(p -> (String) p.getKey(), p -> (String) p.getValue()));
  }

  @Override
  public Optional<ConfigurationProperty> provide(String configurationAttributeKey) {
    Object value = properties.get(configurationAttributeKey);
    if (value == null) {
      return empty();
    }

    return of(new DefaultConfigurationProperty("system property", configurationAttributeKey, value));
  }

  @Override
  public String getDescription() {
    return "system properties provider";
  }
}
