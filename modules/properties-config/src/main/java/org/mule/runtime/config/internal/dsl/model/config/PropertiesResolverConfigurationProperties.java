/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import static java.lang.Boolean.valueOf;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolverProvider;

import java.util.Optional;

import org.slf4j.Logger;

/**
 * Implementations of {@link ConfigurationProperties} that works based on a {@link ConfigurationPropertiesResolver}
 */
public class PropertiesResolverConfigurationProperties
    implements ConfigurationPropertiesResolverProvider, Initialisable, Disposable {

  private static final Logger LOGGER = getLogger(PropertiesResolverConfigurationProperties.class);

  private final ConfigurationPropertiesResolver configurationPropertiesResolver;

  /**
   * Creates a new instance.
   *
   * @param configurationPropertiesResolver the resolver to use for getting the value of the property.
   */
  public PropertiesResolverConfigurationProperties(ConfigurationPropertiesResolver configurationPropertiesResolver) {
    this.configurationPropertiesResolver = configurationPropertiesResolver;
  }

  @Override
  public Optional<Object> resolveProperty(String propertyKey) {
    try {
      return ofNullable(configurationPropertiesResolver.resolvePlaceholderKeyValue(propertyKey));
    } catch (PropertyNotFoundException e) {
      return empty();
    }
  }

  @Override
  public Optional<Boolean> resolveBooleanProperty(String property) {
    return resolveProperty(property).flatMap(value -> {
      if (value instanceof Boolean) {
        return of((Boolean) value);
      }
      if (value instanceof String) {
        return of(valueOf((String) value));
      }
      throw new MuleRuntimeException(createStaticMessage(format("Property %s with value %s cannot be converted to boolean",
                                                                property, value)));
    });
  }

  @Override
  public Optional<String> resolveStringProperty(String property) {
    return resolveProperty(property).map(value -> value instanceof String ? (String) value : value.toString());
  }

  @Override
  public ConfigurationPropertiesResolver getConfigurationPropertiesResolver() {
    return configurationPropertiesResolver;
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(configurationPropertiesResolver);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(configurationPropertiesResolver, LOGGER);
  }
}
