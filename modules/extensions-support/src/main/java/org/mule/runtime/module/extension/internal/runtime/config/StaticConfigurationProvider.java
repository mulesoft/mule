/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedFieldsStream;

import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
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
    return getAnnotatedFieldsStream(configuration.getValue().getClass(), Inject.class)
        .map(field -> {
          Named name = field.getAnnotation(Named.class);
          return name != null
              ? Either.<Class<?>, String>right(name.value())
              : Either.<Class<?>, String>left(field.getType());
        }).collect(toList());
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code false}
   */
  @Override
  public boolean isDynamic() {
    return false;
  }
}
