/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.createInterceptors;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getConnectedComponents;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.injectConfigName;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.InterceptorFactory;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.Optional;

/**
 * Reusable and thread-safe factory that creates instances of {@link ConfigurationInstance}
 * <p>
 * The created instances will be of concrete type {@link LifecycleAwareConfigurationInstance}, which means that all the
 * {@link InterceptorFactory interceptor factories} obtained through {@link RuntimeConfigurationModel#getInterceptorFactories()}
 * will be exercised per each created instance
 *
 * @param <T> the generic type of the returned {@link ConfigurationInstance} instances
 * @since 4.0
 */
public final class ConfigurationInstanceFactory<T> {

  private static final ValueResolver<ConnectionProvider> NULL_CONNECTION_PROVIDER = new StaticValueResolver<>(null);

  private final RuntimeConfigurationModel configurationModel;
  private final ConfigurationObjectBuilder<T> configurationObjectBuilder;
  private final ImplicitConnectionProviderFactory implicitConnectionProviderFactory =
      new DefaultImplicitConnectionProviderFactory();
  private final boolean requiresConnection;

  /**
   * Creates a new instance which provides instances derived from the given {@code configurationModel} and {@code resolverSet}
   *
   * @param configurationModel the {@link ConfigurationModel} that describes the configurations to be created
   * @param resolverSet the {@link ResolverSet} which provides the values for the configuration's parameters
   */
  public ConfigurationInstanceFactory(RuntimeConfigurationModel configurationModel, ResolverSet resolverSet) {
    this.configurationModel = configurationModel;
    configurationObjectBuilder = new ConfigurationObjectBuilder<>(configurationModel, resolverSet);
    requiresConnection = !getConnectedComponents(configurationModel).isEmpty();
  }

  /**
   * Creates a new instance using the given {@code event} to obtain the configuration's parameter values.
   * <p>
   * This method tries to automatically infer a {@link ConnectionProvider}. The first step is to determine if the
   * {@link ExtensionModel} which owns the {@link #configurationModel} has any {@link OperationModel} which requires a connection.
   * If the answer is no, then it just provides a {@code null} connection provider. Otherwise, it uses a
   * {@link ImplicitConnectionProviderFactory} to construct an implicit {@link ConnectionProvider}.
   *
   * @param name the name of the configuration to return
   * @param event the current {@link MuleEvent}
   * @return a {@link ConfigurationInstance}
   * @throws MuleException if an error is encountered
   */
  public ConfigurationInstance<T> createConfiguration(String name, MuleEvent event) throws MuleException {
    ValueResolver<ConnectionProvider> providerResolver;
    if (requiresConnection) {
      providerResolver = new StaticValueResolver<>(implicitConnectionProviderFactory
          .createImplicitConnectionProvider(name, configurationModel, event));
    } else {
      providerResolver = NULL_CONNECTION_PROVIDER;
    }

    return createConfiguration(name, event, providerResolver);
  }

  /**
   * Creates a new instance using the given {@code event} to obtain the configuration's parameter values.
   * <p>
   * This method overload allows specifying a {@link ValueResolver} to provide the {@link ConnectionProvider} that the
   * configuration will use to obtain connections. If the connection does not need such a concept you can provide a {@code null}
   *
   * @param name the name of the configuration to return
   * @param event the current {@link MuleEvent}
   * @param connectionProviderResolver a {@link ValueResolver} to provide the {@link ConnectionProvider} or {@code null}
   * @return a {@link ConfigurationInstance}
   * @throws MuleException if an error is encountered
   */
  public ConfigurationInstance<T> createConfiguration(String name, MuleEvent event,
                                                      ValueResolver<ConnectionProvider> connectionProviderResolver)
      throws MuleException {
    Optional<ConnectionProvider> connectionProvider = Optional.ofNullable(connectionProviderResolver.resolve(event));
    T configValue = createConfigurationInstance(name, event);

    return new LifecycleAwareConfigurationInstance<>(name, configurationModel, configValue,
                                                     createInterceptors(configurationModel), connectionProvider);
  }

  /**
   * Creates a new instance using the given {@code resolverSetResult} to obtain the configuration's parameter values
   *
   * @param name the name of the configuration to return
   * @param resolverSetResult the {@link ResolverSetResult} with previously evaluated values
   * @return a {@link ConfigurationInstance}
   * @throws MuleException if an error is encountered
   */
  public ConfigurationInstance<T> createConfiguration(String name, ResolverSetResult resolverSetResult,
                                                      Optional<ConnectionProvider> connectionProvider)
      throws MuleException {
    T configValue = createConfigurationInstance(name, resolverSetResult);

    return new LifecycleAwareConfigurationInstance<>(name, configurationModel, configValue,
                                                     createInterceptors(configurationModel), connectionProvider);
  }

  private T createConfigurationInstance(String name, ResolverSetResult resolverSetResult) throws MuleException {
    T config = configurationObjectBuilder.build(resolverSetResult);
    injectConfigName(configurationModel, config, name);

    return config;
  }

  private T createConfigurationInstance(String name, MuleEvent event) throws MuleException {
    T config = configurationObjectBuilder.build(event);
    injectConfigName(configurationModel, config, name);

    return config;
  }
}
