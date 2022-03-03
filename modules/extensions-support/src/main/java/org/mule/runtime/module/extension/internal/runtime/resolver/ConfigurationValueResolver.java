/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

/**
 * A value resolver which returns the values of a {@link ConfigurationInstance} obtained through a {@link ConfigurationProvider}
 *
 * @param <T> the generic type of the resolved values
 * @since 4.1
 */
public class ConfigurationValueResolver<T> implements ValueResolver<T> {

  private final ConfigurationProvider configurationProvider;

  public ConfigurationValueResolver(ConfigurationProvider configurationProvider) {
    this.configurationProvider = configurationProvider;
  }

  @Override
  public T resolve(ValueResolvingContext context) throws MuleException {
    ConfigurationInstance configurationInstance = configurationProvider.get(context.getEvent());
    return (T) configurationInstance.getValue();
  }

  @Override
  public boolean isDynamic() {
    return configurationProvider.isDynamic();
  }
}
