/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.Pair;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Optional;

/**
 * Resolves attribute placeholders.
 * <p>
 * It will delegate the placeholder resolution to it's parent if it weren't able to resolve a value by itself.
 *
 * @since 4.0
 */
public class DefaultConfigurationPropertiesResolver implements ConfigurationPropertiesResolver {

  public static final String PLACEHOLDER_PREFIX = "${";
  public static final String PLACEHOLDER_SUFFIX = "}";
  private final Optional<ConfigurationPropertiesResolver> parentResolver;
  private final ConfigurationPropertiesProvider configurationPropertiesProvider;
  private Cache<String, Object> resolutionCache = CacheBuilder.<String, String>newBuilder().build();

  public DefaultConfigurationPropertiesResolver(Optional<ConfigurationPropertiesResolver> parentResolver,
                                                ConfigurationPropertiesProvider configurationPropertiesProvider) {
    this.parentResolver = parentResolver;
    this.configurationPropertiesProvider = configurationPropertiesProvider;
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
        int prefixIndex = value.indexOf(PLACEHOLDER_PREFIX);
        if (prefixIndex == -1) {
          return value;
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

  private Object replaceAllPlaceholders(String value) {
    String innerPlaceholderKey;
    String testValue = value;
    int prefixIndex = testValue.indexOf(PLACEHOLDER_PREFIX);
    while (prefixIndex != -1) {
      int suffixIndex = testValue.indexOf(PLACEHOLDER_SUFFIX, prefixIndex + PLACEHOLDER_PREFIX.length());
      innerPlaceholderKey = testValue.substring(prefixIndex + PLACEHOLDER_PREFIX.length(), suffixIndex);
      Object objectValueFound = resolvePlaceholderKeyValue(innerPlaceholderKey);
      // only use the value as string if it's a concat of placeholders
      if (value.equals(PLACEHOLDER_PREFIX + innerPlaceholderKey + PLACEHOLDER_SUFFIX)) {
        return objectValueFound;
      }
      testValue = testValue.replace(PLACEHOLDER_PREFIX + innerPlaceholderKey + PLACEHOLDER_SUFFIX,
                                    objectValueFound.toString());
      prefixIndex = testValue.indexOf(PLACEHOLDER_PREFIX);
    }
    return testValue;
  }

}
