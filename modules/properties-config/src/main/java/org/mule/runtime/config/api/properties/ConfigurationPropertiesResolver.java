/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.properties;

import org.mule.runtime.config.internal.model.dsl.config.PropertyNotFoundException;

import java.util.function.UnaryOperator;

public interface ConfigurationPropertiesResolver extends UnaryOperator<String> {

  /**
   * Resolves a value by searching and replacing placeholders on it.
   *
   * @param value a value that may contain placeholders.
   * @return if the input value is null, then the result will be null. If the value doesn't have placeholders, then the same value
   *         will be returned. Otherwise placeholders will be resolved.
   */
  Object resolveValue(String value);

  /**
   * Resolves the possible value of a placeholder key.
   *
   * @param placeholderKey the placeholder key which value needs to be resolved.
   * @return the resolved value.
   */
  Object resolvePlaceholderKeyValue(final String placeholderKey);

  @Override
  default public String apply(String t) {
    try {
      final Object resolved = resolveValue(t);
      return resolved == null ? null : resolved.toString();
    } catch (PropertyNotFoundException e) {
      return t;
    }
  }

}
