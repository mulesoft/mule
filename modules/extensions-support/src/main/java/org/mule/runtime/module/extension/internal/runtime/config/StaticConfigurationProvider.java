/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;

/**
 * {@link ConfigurationProvider} which provides always the same {@link #configuration}.
 *
 * @param <T> the generic type of the instances provided
 * @since 3.7.0
 */
public final class StaticConfigurationProvider<T> extends LifecycleAwareConfigurationProvider<T> {

  private final ConfigurationInstance<T> configuration;

  public StaticConfigurationProvider(String name, RuntimeConfigurationModel model, ConfigurationInstance<T> configuration) {
    super(name, model);
    this.configuration = configuration;
    registerConfiguration(configuration);
  }

  /**
   * Returns {@link #configuration}.
   *
   * @param muleEvent the current {@link MuleEvent}
   * @return {@link #configuration}
   */
  @Override
  public ConfigurationInstance<T> get(Object muleEvent) {
    return configuration;
  }
}
