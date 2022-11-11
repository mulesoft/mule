/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import static java.lang.Boolean.valueOf;
import static java.lang.System.getProperty;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.properties.api.ConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ConfigurationProperty;

import java.util.Optional;

import org.slf4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Resolves attribute placeholders.
 * <p>
 * It will delegate the placeholder resolution to it's parent if it weren't able to resolve a value by itself.
 *
 * @since 4.0
 */
public class DefaultConfigurationPropertiesResolver implements ConfigurationPropertiesResolver, Initialisable, Disposable {

  private static final Logger LOGGER = getLogger(DefaultConfigurationPropertiesResolver.class);
  private static final Boolean CORRECT_USE_OF_BACKSLASH = valueOf(getProperty("mule.properties.correct.backslash.use", "true"));
  public static final String PLACEHOLDER_PREFIX = "${";
  public static final String PLACEHOLDER_SUFFIX = "}";
  private final Optional<ConfigurationPropertiesResolver> nextResolver;
  private final ConfigurationPropertiesProvider configurationPropertiesProvider;
  private final Cache<String, Object> resolutionCache = CacheBuilder.<String, String>newBuilder().build();
  private boolean initialized = false;
  private Optional<ConfigurationPropertiesResolver> rootResolver = empty();
  private final boolean failIfPropertyNotFound;

  public DefaultConfigurationPropertiesResolver(Optional<ConfigurationPropertiesResolver> nextResolver,
                                                ConfigurationPropertiesProvider configurationPropertiesProvider) {
    this(nextResolver, configurationPropertiesProvider, true);
  }

  public DefaultConfigurationPropertiesResolver(Optional<ConfigurationPropertiesResolver> nextResolver,
                                                ConfigurationPropertiesProvider configurationPropertiesProvider,
                                                boolean failIfPropertyNotFound) {
    this.nextResolver = nextResolver;
    this.configurationPropertiesProvider = requireNonNull(configurationPropertiesProvider);
    this.failIfPropertyNotFound = failIfPropertyNotFound;
  }

  private boolean shouldResolvePlaceholder(String value, int prefixIndex) {
    if (prefixIndex == 0) {
      return true;
    } else if (value.charAt(prefixIndex - 1) != '\\') {
      return true;
    } else if (prefixIndex == 1) {
      return false;
    } else if (prefixIndex > 1 && value.charAt(prefixIndex - 2) != '\\') {
      return false;
    } else {
      return true;
    }
  }

  private int findPrefixIndex(String value, int offset) {
    int prefixIndex = value.indexOf(PLACEHOLDER_PREFIX);
    if (prefixIndex == -1) {
      return -1;
    }

    if (shouldResolvePlaceholder(value, prefixIndex)) {
      return prefixIndex + offset;
    } else {
      int relativeOffset = prefixIndex + PLACEHOLDER_PREFIX.length();
      return findPrefixIndex(value.substring(relativeOffset), offset + relativeOffset);
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
  @Override
  public Object resolveValue(String value) {
    if (value == null) {
      return value;
    }
    try {
      return this.resolutionCache.get(value, () -> {
        int prefixIndex = prefixIndexConsideringBackslash(value);
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
    initialiseIfNeeded(nextResolver);
    if (!initialized) {
      initialiseIfNeeded(configurationPropertiesProvider);
      initialized = true;
    }
  }

  @Override
  public void dispose() {
    disposeIfNeeded(configurationPropertiesProvider, LOGGER);
    disposeIfNeeded(nextResolver, LOGGER);
    initialized = false;
  }

  /**
   * Resolves the possible value of a placeholder key.
   *
   * @param placeholderKey the placeholder key which value needs to be resolved.
   * @return the resolved value.
   */
  @Override
  public Object resolvePlaceholderKeyValue(final String placeholderKey) {
    Optional<? extends ConfigurationProperty> foundValueOptional =
        configurationPropertiesProvider.provide(placeholderKey);
    // verify that the provided value is not the same as the placeholder key searched for. If that's the case jump to parent.
    if (foundValueOptional.isPresent()
        && !foundValueOptional.get().getValue().equals(PLACEHOLDER_PREFIX + placeholderKey + PLACEHOLDER_SUFFIX)) {
      if (foundValueOptional.get().getValue() instanceof String) {
        return replaceAllPlaceholders(foundValueOptional.get().getValue());
      } else {
        return foundValueOptional.get();
      }
    } else if (nextResolver.isPresent()) {
      try {
        return nextResolver.get().resolvePlaceholderKeyValue(placeholderKey);
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

  private int prefixIndexConsideringBackslash(String value) {
    return CORRECT_USE_OF_BACKSLASH ? findPrefixIndex(value) : value.indexOf(PLACEHOLDER_PREFIX);
  }

  private Object replaceAllPlaceholders(String value) {
    String innerPlaceholderKey;
    String testValue = value;
    int prefixIndex = prefixIndexConsideringBackslash(value);
    while (prefixIndex != -1) {
      int suffixIndex = testValue.indexOf(PLACEHOLDER_SUFFIX, prefixIndex + PLACEHOLDER_PREFIX.length());
      innerPlaceholderKey = testValue.substring(prefixIndex + PLACEHOLDER_PREFIX.length(), suffixIndex);
      Object objectValueFound = tryResolveByRoot(innerPlaceholderKey);
      // only use the value as string if it's a concat of placeholders
      if (value.equals(PLACEHOLDER_PREFIX + innerPlaceholderKey + PLACEHOLDER_SUFFIX)) {
        return objectValueFound;
      }
      // Avoid propagating the escaped backslash
      if (prefixIndex > 1 && testValue.charAt(prefixIndex - 1) == '\\' && testValue.charAt(prefixIndex - 2) == '\\') {
        prefixIndex--;
      }
      testValue = testValue.substring(0, prefixIndex) + objectValueFound.toString() + testValue.substring(suffixIndex + 1);

      prefixIndex = prefixIndexConsideringBackslash(testValue);
    }
    return CORRECT_USE_OF_BACKSLASH ? testValue.replace("\\" + PLACEHOLDER_PREFIX, PLACEHOLDER_PREFIX)
        : testValue;
  }

  private void propagateRootResolver(ConfigurationPropertiesResolver rootResolver) {
    this.nextResolver.ifPresent(resolver -> {
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


  @Override
  public String apply(String t) {
    try {
      final Object resolved = resolveValue(t);
      return resolved == null ? null : resolved.toString();
    } catch (PropertyNotFoundException p) {
      if (failIfPropertyNotFound) {
        throw p;
      }
      return null;
    }
  }
}
