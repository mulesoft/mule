/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.config.ConfigurationException;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;

/**
 * Implementation of {@link ValueResolver} which accesses the mule registry and returns the value associated with {@link #key}.
 * <p/>
 * Although the registry is mutable, {@link #isDynamic()} will always return {@code false} since the value associated to a given
 * key is not meant to change.
 *
 * @since 3.7.0
 */
public class RegistryLookupValueResolver<T> implements ValueResolver<T> {

  private final String key;

  @Inject
  private MuleContext muleContext;

  /**
   * Construct a new instance and set the {@link #key} that will be used to access the registry
   *
   * @param key a not blank {@link String}
   * @param muleContext the Mule node
   */
  public RegistryLookupValueResolver(String key) {
    checkArgument(!StringUtils.isBlank(key), "key cannot be null or blank");
    this.key = key;
  }

  /**
   * Returns the registry value associated with {@link #key}
   *
   * @param context a {@link ValueResolvingContext}
   * @return the registry value associated with {@link #key}
   * @throws MuleException if an error occurred fetching the value
   * @throws ConfigurationException if no object is registered under {@link #key}
   */
  @Override
  public T resolve(ValueResolvingContext context) throws MuleException {
    T value = muleContext.getRegistry().get(key);
    if (value == null) {
      throw new ConfigurationException(createStaticMessage(format("Element '%s' is not defined in the Mule Registry", key)));
    }

    return value;
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isDynamic() {
    return false;
  }

  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }
}
