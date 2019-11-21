/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static java.util.Optional.empty;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProvider;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationProperty;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Optional;

import org.slf4j.Logger;

/**
 * Resolves attribute placeholders.
 * <p>
 * It will delegate the placeholder resolution to it's parent if it weren't able to resolve a value by itself.
 *
 * @since 4.0
 */
public class DefaultConfigurationPropertiesResolver implements ConfigurationPropertiesResolver, Initialisable, Disposable {

  private static final Logger LOGGER = getLogger(DefaultConfigurationPropertiesResolver.class);
  private static final Boolean CORRECT_USE_OF_BACKSLASH = Boolean.valueOf(System.getProperty("mule.properties.correct.backslash.use", "true"));
  public static final String PLACEHOLDER_PREFIX = "${";
  public static final String PLACEHOLDER_SUFFIX = "}";
  private final Optional<ConfigurationPropertiesResolver> parentResolver;
  private final ConfigurationPropertiesProvider configurationPropertiesProvider;
  private Cache<String, Object> resolutionCache = CacheBuilder.<String, String>newBuilder().build();
  private boolean initialized = false;
  private Optional<ConfigurationPropertiesResolver> rootResolver = empty();

  public DefaultConfigurationPropertiesResolver(Optional<ConfigurationPropertiesResolver> parentResolver,
                                                ConfigurationPropertiesProvider configurationPropertiesProvider) {
    this.parentResolver = parentResolver;
    this.configurationPropertiesProvider = configurationPropertiesProvider;
  }

  private int findPrefixIndex(String value, int offset) {
    int prefixIndex = value.indexOf(PLACEHOLDER_PREFIX);
    if (prefixIndex == -1) {
      return -1;
    }
    if (prefixIndex != 0 && value.charAt(prefixIndex - 1) == '\\') {
      int relativeOffset = prefixIndex + PLACEHOLDER_PREFIX.length();
      return findPrefixIndex(value.substring(relativeOffset), offset + relativeOffset);
    } else {
      return prefixIndex + offset;
    }
  }

  private int findPrefixIndex(String value) {
    return findPrefixIndex(value, 0);
  }

  /**
   * Resolves a value by searching and replacing placeholders on it.
   *
   * @param value a value that may contain placeholders.
   * @return if the input value is null, then the result will be null. If the value doesn't have placeholders, then the same value
   *         will be returned. Otherwise placeholders will be resolved.
   */
  public Object resolveValue(String value) {
    if (value == null) {
      return value;
    }
    try {
      return this.resolutionCache.get(value, () -> {
        int prefixIndex = prefix_index_considering_backslash(value);
        if (prefixIndex == -1) {
          return CORRECT_USE_OF_BACKSLASH ? value.replace("\\" + PLACEHOLDER_PREFIX, PLACEHOLDER_PREFIX) : value;
        }
        return replaceAllPlaceholders(value);
      });
    } catch (Exception e) {
      if (e.getCause() instanceof RuntimeException) {
        throw (RuntimeException) e.getCause();
      } else {
        throw new MuleRuntimeException(createStaticMessage("Failure processing configuration attribute " + value, e));
      }
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(parentResolver);
    if (!initialized) {
      initialiseIfNeeded(configurationPropertiesProvider);
      initialized = true;
    }
  }

  @Override
  public void dispose() {
    disposeIfNeeded(configurationPropertiesProvider, LOGGER);
    disposeIfNeeded(parentResolver, LOGGER);
    initialized = false;
  }

  /**
   * Resolves the possible value of a placeholder key.
   * 
   * @param placeholderKey the placeholder key which value needs to be resolved.
   * @return the resolved value.
   */
  public Object resolvePlaceholderKeyValue(final String placeholderKey) {
    Optional<ConfigurationProperty> foundValueOptional =
        configurationPropertiesProvider.getConfigurationProperty(placeholderKey);
    // verify that the provided value is not the same as the placeholder key searched for. If that's the case jump to parent.
    if (foundValueOptional.isPresent()
        && !foundValueOptional.get().getRawValue().equals(PLACEHOLDER_PREFIX + placeholderKey + PLACEHOLDER_SUFFIX)) {
      if (foundValueOptional.get().getRawValue() instanceof String) {
        return replaceAllPlaceholders((String) foundValueOptional.get().getRawValue());
      } else {
        return foundValueOptional.get();
      }
    } else if (parentResolver.isPresent()) {
      try {
        return parentResolver.get().resolvePlaceholderKeyValue(placeholderKey);
      } catch (PropertyNotFoundException e) {
        throw new PropertyNotFoundException(e, new Pair<>(configurationPropertiesProvider.getDescription(), placeholderKey));
      }
    }
    throw new PropertyNotFoundException(new Pair<>(configurationPropertiesProvider.getDescription(), placeholderKey));
  }

  private Object tryResolveByRoot(String placeholder) {
    if (!rootResolver.isPresent()) {
      return resolvePlaceholderKeyValue(placeholder);
    }
    try {
      return rootResolver.get().resolvePlaceholderKeyValue(placeholder);
    } catch (PropertyNotFoundException e) {
      return resolvePlaceholderKeyValue(placeholder);
    }
  }

  private int prefix_index_considering_backslash(String value) {
    return CORRECT_USE_OF_BACKSLASH ? findPrefixIndex(value) : value.indexOf(PLACEHOLDER_PREFIX);
  }

  private Object replaceAllPlaceholders(String value) {
    String innerPlaceholderKey;
    String testValue = value;
      int prefixIndex = prefix_index_considering_backslash(value);
    while (prefixIndex != -1) {
      int suffixIndex = testValue.indexOf(PLACEHOLDER_SUFFIX, prefixIndex + PLACEHOLDER_PREFIX.length());
      innerPlaceholderKey = testValue.substring(prefixIndex + PLACEHOLDER_PREFIX.length(), suffixIndex);
      Object objectValueFound = tryResolveByRoot(innerPlaceholderKey);
      // only use the value as string if it's a concat of placeholders
      if (value.equals(PLACEHOLDER_PREFIX + innerPlaceholderKey + PLACEHOLDER_SUFFIX)) {
        return objectValueFound;
      }
      testValue = testValue.replace(PLACEHOLDER_PREFIX + innerPlaceholderKey + PLACEHOLDER_SUFFIX,
                                    objectValueFound.toString());
      prefixIndex = prefix_index_considering_backslash(testValue);
    }
    return CORRECT_USE_OF_BACKSLASH ? testValue.replace("\\" + PLACEHOLDER_PREFIX, PLACEHOLDER_PREFIX) : testValue;
  }

  private void propagateRootResolver(ConfigurationPropertiesResolver rootResolver) {
    this.parentResolver.ifPresent(resolver -> {
      if (resolver instanceof DefaultConfigurationPropertiesResolver) {
        ((DefaultConfigurationPropertiesResolver) resolver).setRootResolver(rootResolver);
      }
    });
  }

  public void setRootResolver(ConfigurationPropertiesResolver rootResolver) {
    this.rootResolver = of(rootResolver);
    propagateRootResolver(rootResolver);
  }

  public void setAsRootResolver() {
    propagateRootResolver(this);
  }
}
