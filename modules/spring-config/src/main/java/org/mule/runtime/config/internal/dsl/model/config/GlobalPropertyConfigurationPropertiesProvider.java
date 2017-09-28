/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.Optional;

public class GlobalPropertyConfigurationPropertiesProvider implements ConfigurationPropertiesProvider {

  private Map<String, ConfigurationProperty> globalPropertiesConfigurationAttributes;

  public GlobalPropertyConfigurationPropertiesProvider(Map<String, ConfigurationProperty> properties) {
    this.globalPropertiesConfigurationAttributes = properties;
  }


  @Override
  public Optional<ConfigurationProperty> getConfigurationProperty(String configurationAttributeKey) {
    return ofNullable(globalPropertiesConfigurationAttributes.get(configurationAttributeKey));
  }

  @Override
  public String getDescription() {
    return "global-properties configuration attributes";
  }

}
