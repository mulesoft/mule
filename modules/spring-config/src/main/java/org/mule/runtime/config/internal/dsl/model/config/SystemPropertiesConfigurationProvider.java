/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

public class SystemPropertiesConfigurationProvider implements ConfigurationPropertiesProvider {

  private Map<String, ConfigurationProperty> configurationAttributes = new HashMap<>();

  public SystemPropertiesConfigurationProvider() {
    Properties properties = System.getProperties();
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
    return "system properties provider";
  }
}
