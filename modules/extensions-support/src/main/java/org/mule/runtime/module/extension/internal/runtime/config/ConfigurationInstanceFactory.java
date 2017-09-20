/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getConnectedComponents;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext.from;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.createInterceptors;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.injectFields;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.InterceptorFactory;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationState;
import org.mule.runtime.module.extension.internal.loader.java.property.InterceptorsModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Reusable and thread-safe factory that creates instances of {@link ConfigurationInstance}
 * <p>
 * The created instances will be of concrete type {@link LifecycleAwareConfigurationInstance}, which means that all the
 * {@link InterceptorFactory interceptor factories} obtained through the {@link InterceptorsModelProperty}
 * (if present) will be exercised per each created instance
 *
 * @param <T> the generic type of the returned {@link ConfigurationInstance} instances
 * @since 4.0
 */
public final class ConfigurationInstanceFactory<T> {

  private final ConfigurationModel configurationModel;
  private final ConfigurationObjectBuilder<T> configurationObjectBuilder;
  private final ImplicitConnectionProviderFactory implicitConnectionProviderFactory;
  private final boolean requiresConnection;
  private final MuleContext muleContext;

  /**
   * Creates a new instance which provides instances derived from the given {@code configurationModel} and {@code resolverSet}
   *
   * @param extensionModel     the {@link ExtensionModel} that owns the {@code configurationModel}
   * @param configurationModel the {@link ConfigurationModel} that describes the configurations to be created
   * @param resolverSet        the {@link ResolverSet} which provides the values for the configuration's parameters
   * @param muleContext        the current {@link MuleContext}
   */
  public ConfigurationInstanceFactory(ExtensionModel extensionModel, ConfigurationModel configurationModel,
                                      ResolverSet resolverSet, MuleContext muleContext) {
    this.configurationModel = configurationModel;
    this.muleContext = muleContext;
    configurationObjectBuilder = new ConfigurationObjectBuilder<>(configurationModel, resolverSet);
    requiresConnection = !getConnectedComponents(extensionModel, configurationModel).isEmpty();
    implicitConnectionProviderFactory =
        new DefaultImplicitConnectionProviderFactory(extensionModel, configurationModel, muleContext);
  }

  /**
   * Creates a new instance using the given {@code event} to obtain the configuration's parameter values.
   * <p>
   * This method overload allows specifying a {@link ValueResolver} to provide the {@link ConnectionProvider} that the
   * configuration will use to obtain connections. If the connection does not need such a concept you can provide a {@code null}
   *
   * @param name                       the name of the configuration to return
   * @param event                      the current {@link CoreEvent}
   * @param connectionProviderResolver a {@link ValueResolver} to provide the {@link ConnectionProvider} or {@code null}
   * @return a {@link ConfigurationInstance}
   * @throws MuleException if an error is encountered
   */
  public <C> ConfigurationInstance createConfiguration(String name,
                                                       CoreEvent event,
                                                       ConnectionProviderValueResolver<C> connectionProviderResolver)
      throws MuleException {

    Optional<Pair<ConnectionProvider<C>, ResolverSetResult>> connectionProvider;

    Pair<T, ResolverSetResult> configValue = createConfigurationInstance(name, event);
    if (requiresConnection) {
      connectionProvider = Optional.ofNullable(connectionProviderResolver.resolve(from(event)));
    } else {
      connectionProvider = empty();
    }

    return new LifecycleAwareConfigurationInstance(name,
                                                   configurationModel,
                                                   configValue.getFirst(),
                                                   createState(configValue.getSecond(), connectionProvider),
                                                   createInterceptors(configurationModel),
                                                   connectionProvider.map(Pair::getFirst));
  }

  /**
   * Creates a new instance using the given {@code resolverSetResult} to obtain the configuration's parameter values
   *
   * @param name                       the name of the configuration to return
   * @param configValues               the {@link ResolverSetResult} with the evaluated config parameters values
   * @param event                      the current {@link CoreEvent}
   * @param connectionProviderResolver an optional resolver to obtain a {@link ConnectionProvider}
   * @return a {@link ConfigurationInstance}
   * @throws MuleException if an error is encountered
   */
  public <C> ConfigurationInstance createConfiguration(String name,
                                                       ResolverSetResult configValues,
                                                       CoreEvent event,
                                                       Optional<ConnectionProviderValueResolver<C>> connectionProviderResolver)
      throws MuleException {

    Pair<T, ResolverSetResult> configValue = createConfigurationInstance(name, configValues);

    Optional<Pair<ConnectionProvider<C>, ResolverSetResult>> connectionProvider;

    if (requiresConnection && connectionProviderResolver.isPresent()) {
      connectionProvider = ofNullable(connectionProviderResolver.get().resolve(from(event)));
    } else {
      connectionProvider = empty();
    }

    return new LifecycleAwareConfigurationInstance(name,
                                                   configurationModel,
                                                   configValue.getFirst(),
                                                   createState(configValue.getSecond(), connectionProvider),
                                                   createInterceptors(configurationModel),
                                                   connectionProvider.map(Pair::getFirst));
  }

  /**
   * Creates a new instance using the given {@code configValues} and {@code connectionProviderValues} to obtain the
   * configuration's parameter values
   *
   * @param name                       the name of the configuration to return
   * @param configValues               the {@link ResolverSetResult} with the evaluated config parameters values
   * @param event                      the current {@link CoreEvent}
   * @param connectionProviderResolver a resolver to obtain a {@link ConnectionProvider}
   * @param connectionProviderValues   e {@link ResolverSetResult} with the evaluated connection parameters values
   * @return a {@link ConfigurationInstance}
   * @throws MuleException if an error is encountered
   */
  public <C> ConfigurationInstance createConfiguration(String name,
                                                       ResolverSetResult configValues,
                                                       CoreEvent event,
                                                       ConnectionProviderValueResolver<C> connectionProviderResolver,
                                                       ResolverSetResult connectionProviderValues)
      throws MuleException {
    Pair<T, ResolverSetResult> configValue = createConfigurationInstance(name, configValues);

    Optional<Pair<ConnectionProvider<C>, ResolverSetResult>> connectionProvider = empty();

    if (requiresConnection && connectionProviderResolver != null) {
      if (connectionProviderResolver.getObjectBuilder().isPresent()) {
        connectionProvider = ofNullable(connectionProviderResolver.getObjectBuilder().get().build(connectionProviderValues));
      }

      if (!connectionProvider.isPresent()) {
        connectionProvider = ofNullable(connectionProviderResolver.resolve(from(event)));
      }
    }

    return new LifecycleAwareConfigurationInstance(name,
                                                   configurationModel,
                                                   configValue.getFirst(),
                                                   createState(configValue.getSecond(), connectionProvider),
                                                   createInterceptors(configurationModel),
                                                   connectionProvider.map(Pair::getFirst));
  }

  private Pair<T, ResolverSetResult> createConfigurationInstance(String name, ResolverSetResult resolverSetResult)
      throws MuleException {
    Pair<T, ResolverSetResult> config = configurationObjectBuilder.build(resolverSetResult);
    injectFields(configurationModel, config.getFirst(), name, muleContext.getConfiguration().getDefaultEncoding());

    return config;
  }

  private Pair<T, ResolverSetResult> createConfigurationInstance(String name, CoreEvent event) throws MuleException {
    Pair<T, ResolverSetResult> config = configurationObjectBuilder.build(from(event));
    injectFields(configurationModel, config.getFirst(), name, muleContext.getConfiguration().getDefaultEncoding());

    return config;
  }

  private <C> ConfigurationState createState(ResolverSetResult configValues,
                                             Optional<Pair<ConnectionProvider<C>, ResolverSetResult>> connectionProvider) {
    return new ImmutableConfigurationState(
                                           nullSafeMap(configValues),
                                           connectionProvider.map(p -> nullSafeMap(p.getSecond()))
                                               .orElseGet(Collections::emptyMap));
  }

  private Map<String, Object> nullSafeMap(ResolverSetResult result) {
    return result.asMap().entrySet().stream()
        .filter(entry -> entry.getValue() != null)
        .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
  }
}
