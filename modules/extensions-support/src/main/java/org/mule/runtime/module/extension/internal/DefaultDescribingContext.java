/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * Default implementation of {@link DescribingContext}. The fact that this class's attributes are immutable, doesn't mean that
 * their inner state is in fact immutable also.
 *
 * @since 3.7.0
 */
public final class DefaultDescribingContext implements DescribingContext {

  private final ExtensionDeclarer extensionDeclarer;
  private final Map<String, Object> customParameters = new HashMap<>();

  public DefaultDescribingContext(ClassLoader classLoader) {
    this(new ExtensionDeclarer(), classLoader);

  }

  public DefaultDescribingContext(ExtensionDeclarer extensionDeclarer, ClassLoader classLoader) {
    this.extensionDeclarer = extensionDeclarer;
    addParameter(ExtensionProperties.EXTENSION_CLASSLOADER, classLoader);
  }

  /**
   * {@inheritDoc}
   */
  public ExtensionDeclarer getExtensionDeclarer() {
    return extensionDeclarer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addParameter(String key, Object value) {
    checkArgument(!StringUtils.isBlank(key), "key cannot be blank");
    checkArgument(value != null, "value cannot be null");

    customParameters.put(key, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T getParameter(String key, Class<T> expectedType) {
    Object parameter = customParameters.get(key);
    if (parameter == null) {
      return null;
    }

    if (!expectedType.isInstance(parameter)) {
      throw new IllegalArgumentException(String.format(
                                                       "Custom parameter '%s' was expected to be of class '%s' but got '%s' instead",
                                                       key, expectedType.getName(), parameter.getClass().getName()));
    }

    return (T) parameter;
  }
}
