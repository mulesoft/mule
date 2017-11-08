/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedFields;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.internal.lifecycle.InjectedDependenciesProvider;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * {@link ConfigurationProvider} which provides always the same {@link #configuration}.
 *
 * @since 3.7.0
 */
public class StaticConfigurationProvider extends LifecycleAwareConfigurationProvider implements InjectedDependenciesProvider {

  private final ConfigurationInstance configuration;

  public StaticConfigurationProvider(String name, ExtensionModel extensionModel, ConfigurationModel configurationModel,
                                     ConfigurationInstance configuration, MuleContext muleContext) {
    super(name, extensionModel, configurationModel, muleContext);
    this.configuration = configuration;
    registerConfiguration(configuration);
  }

  /**
   * Returns {@link #configuration}.
   *
   * @param muleEvent the current {@link CoreEvent}
   * @return {@link #configuration}
   */
  @Override
  public ConfigurationInstance get(Event muleEvent) {
    return configuration;
  }

  @Override
  public Collection<Either<Class<?>, String>> getInjectedDependencies() {
    return getAnnotatedFields(configuration.getValue().getClass(), Inject.class).stream()
        .map(field -> {
          Named name = field.getAnnotation(Named.class);
          return name != null
              ? Either.<Class<?>, String>right(name.value())
              : Either.<Class<?>, String>left(field.getType());
        }).collect(toList());
  }
}
