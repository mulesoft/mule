/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.dsl.config;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import static java.util.Optional.empty;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.properties.api.ConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ConfigurationProperty;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;

public class CompositeConfigurationPropertiesProvider implements ConfigurationPropertiesProvider, Initialisable, Disposable {

  private static final Logger LOGGER = getLogger(CompositeConfigurationPropertiesProvider.class);
  private final List<ConfigurationPropertiesProvider> configurationPropertiesProviders;
  private boolean initialized = false;
  private boolean disposed = false;

  @Override
  public void dispose() {
    if (disposed) {
      return;
    }
    disposeIfNeeded(configurationPropertiesProviders, LOGGER);
    disposed = true;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (initialized) {
      return;
    }
    for (ConfigurationPropertiesProvider configurationPropertiesProvider : configurationPropertiesProviders) {
      initialiseIfNeeded(configurationPropertiesProvider);
    }
    initialized = true;
  }

  public CompositeConfigurationPropertiesProvider(List<ConfigurationPropertiesProvider> configurationPropertiesProviders) {
    this.configurationPropertiesProviders = configurationPropertiesProviders;
  }

  @Override
  public Optional<? extends ConfigurationProperty> provide(String configurationAttributeKey) {
    for (ConfigurationPropertiesProvider configurationPropertiesProvider : configurationPropertiesProviders) {
      Optional<? extends ConfigurationProperty> configurationAttribute =
          configurationPropertiesProvider.provide(configurationAttributeKey);
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

  @Override
  public String toString() {
    return getDescription();
  }
}
