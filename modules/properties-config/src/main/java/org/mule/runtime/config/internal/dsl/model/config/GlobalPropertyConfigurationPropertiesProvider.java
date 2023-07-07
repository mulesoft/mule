/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

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
