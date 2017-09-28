/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import static java.util.Optional.empty;

import java.util.List;
import java.util.Optional;

public class CompositeConfigurationPropertiesProvider implements ConfigurationPropertiesProvider {

  private List<ConfigurationPropertiesProvider> configurationPropertiesProviders;

  public CompositeConfigurationPropertiesProvider(List<ConfigurationPropertiesProvider> configurationPropertiesProviders) {
    this.configurationPropertiesProviders = configurationPropertiesProviders;
  }

  @Override
  public Optional<ConfigurationProperty> getConfigurationProperty(String configurationAttributeKey) {
    for (ConfigurationPropertiesProvider configurationPropertiesProvider : configurationPropertiesProviders) {
      Optional<ConfigurationProperty> configurationAttribute =
          configurationPropertiesProvider.getConfigurationProperty(configurationAttributeKey);
      if (configurationAttribute.isPresent()) {
        return configurationAttribute;
      }
    }
    return empty();
  }

  @Override
  public String getDescription() {
    StringBuilder stringBuilder = new StringBuilder("configuration-attribute provider composed of (");
    for (ConfigurationPropertiesProvider configurationPropertiesProvider : configurationPropertiesProviders) {
      stringBuilder.append(configurationPropertiesProvider.getDescription());
      stringBuilder.append(", ");
    }
    stringBuilder.replace(stringBuilder.length() - 2, stringBuilder.length(), "");
    return stringBuilder.append(")").toString();
  }
}
