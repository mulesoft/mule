/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;

/**
 * A {@link ConfigurationProvider} that defers the resolution of the actual {@link ConfigurationProvider} to be used until an
 * event is received.
 *
 * @since 4.5.0
 */
public final class DeferredConfigurationProvider extends AbstractComponent implements ConfigurationProvider {

  /**
   *
   * @param extensionModel the model that owns the {@link ConfigurationModel}.
   * @param operationModel the model for the component that requires the configuration.
   * @return A {@link ConfigurationModel} suitable for the given {@link ComponentModel}.
   */
  private static ConfigurationModel getConfigurationModelForComponent(ExtensionModel extensionModel,
                                                                      ComponentModel operationModel) {
    return extensionModel.getConfigurationModels().stream()
        .filter(cm -> cm.getOperationModels().stream().anyMatch(om -> om.getName().equals(operationModel.getName())))
        .findAny()
        .orElse(null);
  }

  private final String name;
  private final ExtensionModel extensionModel;
  private final ConfigurationModel configurationModel;
  private final ClassLoader extensionClassLoader;
  private final ValueResolver<ConfigurationProvider> expressionResolver;
  private final ExpressionManager expressionManager;

  /**
   * Creates a new instance
   *
   * @param extensionModel        the model that owns the {@link ConfigurationModel} for the instances that are going to be
   *                              provided.
   * @param componentModel        the model for the component that requires the configuration, used to determine the right
   *                              {@link ConfigurationModel}.
   * @param configurationResolver the {@link ValueResolver} that should resolve to a {@link ConfigurationProvider} instance.
   * @param expressionManager     the {@link ExpressionManager} used to create a session used to evaluate the attributes.
   */
  public DeferredConfigurationProvider(ExtensionModel extensionModel,
                                       ComponentModel componentModel,
                                       ValueResolver<ConfigurationProvider> configurationResolver,
                                       ExpressionManager expressionManager) {
    this.name = extensionModel.getName();
    this.extensionModel = extensionModel;
    this.configurationModel = getConfigurationModelForComponent(extensionModel, componentModel);
    this.extensionClassLoader = getClassLoader(extensionModel);
    this.expressionResolver = configurationResolver;
    this.expressionManager = expressionManager;
  }

  /**
   * Evaluates {@link #expressionResolver} using the given {@code event} to get a {@link ConfigurationProvider} and then gets an
   * instance from it.
   *
   * @param event the current {@code event}
   * @return the resolved {@link ConfigurationInstance}
   */
  @Override
  public ConfigurationInstance get(Event event) {
    return withContextClassLoader(extensionClassLoader, () -> {
      try (ValueResolvingContext resolvingContext = ValueResolvingContext.builder(((CoreEvent) event))
          .withExpressionManager(expressionManager).build()) {
        ConfigurationProvider configurationProvider = expressionResolver.resolve(resolvingContext);
        if (configurationProvider != null) {
          return configurationProvider.get(event);
        }
      }
      return null;
    });
  }

  @Override
  public ExtensionModel getExtensionModel() {
    return extensionModel;
  }

  @Override
  public ConfigurationModel getConfigurationModel() {
    return configurationModel;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code false}
   */
  @Override
  public boolean isDynamic() {
    return true;
  }
}
