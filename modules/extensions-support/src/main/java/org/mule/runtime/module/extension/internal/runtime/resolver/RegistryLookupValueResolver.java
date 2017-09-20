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

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.config.ConfigurationException;

import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

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

  private Registry registry;

  /**
   * Construct a new instance and set the {@link #key} that will be used to access the registry
   *
   * @param key a not blank {@link String}
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
    return registry.<T>lookupByName(key)
        .orElseThrow(() -> new ConfigurationException(createStaticMessage(format("Element '%s' is not defined in the Mule Registry",
                                                                                 key))));
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isDynamic() {
    return false;
  }

  @Inject
  public void setRegistry(Registry registry) {
    this.registry = registry;
  }
}
