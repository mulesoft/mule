/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getConnectedComponents;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext.from;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.createInterceptors;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.injectRefName;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.toMap;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.extension.api.runtime.InterceptorFactory;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationState;
import org.mule.runtime.module.extension.internal.loader.java.property.InterceptorsModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;

import java.util.Map;
import java.util.Optional;

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

  private static final ValueResolver<ConnectionProvider> NULL_CONNECTION_PROVIDER = new StaticValueResolver<>(null);

  private final ExtensionModel extensionModel;
  private final ConfigurationModel configurationModel;
  private final ConfigurationObjectBuilder<T> configurationObjectBuilder;
  private final ImplicitConnectionProviderFactory implicitConnectionProviderFactory;
  private final ResolverSet resolverSet;
  private final boolean requiresConnection;

  /**
   * Creates a new instance which provides instances derived from the given {@code configurationModel} and {@code resolverSet}
   *
   * @param configurationModel the {@link ConfigurationModel} that describes the configurations to be created
   * @param resolverSet the {@link ResolverSet} which provides the values for the configuration's parameters
   */
  public ConfigurationInstanceFactory(ExtensionModel extensionModel, ConfigurationModel configurationModel,
                                      ResolverSet resolverSet, MuleContext muleContext) {
    this.extensionModel = extensionModel;
    this.configurationModel = configurationModel;
    this.resolverSet = resolverSet;
    configurationObjectBuilder = new ConfigurationObjectBuilder<>(configurationModel, resolverSet);
    requiresConnection = !getConnectedComponents(extensionModel, configurationModel).isEmpty();
    implicitConnectionProviderFactory =
        new DefaultImplicitConnectionProviderFactory(extensionModel, configurationModel, muleContext);
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
   * @param event the current {@link Event}
   * @return a {@link ConfigurationInstance}
   * @throws MuleException if an error is encountered
   */
  public ConfigurationInstance createConfiguration(String name, Event event)
      throws MuleException {
    ValueResolver<ConnectionProvider> providerResolver;
    if (requiresConnection) {
      providerResolver =
          new StaticValueResolver<>(implicitConnectionProviderFactory.createImplicitConnectionProvider(name, event));
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
   * @param event the current {@link Event}
   * @param connectionProviderResolver a {@link ValueResolver} to provide the {@link ConnectionProvider} or {@code null}
   * @return a {@link ConfigurationInstance}
   * @throws MuleException if an error is encountered
   */
  public ConfigurationInstance createConfiguration(String name, Event event,
                                                   ValueResolver<ConnectionProvider> connectionProviderResolver)
      throws MuleException {

    if (!requiresConnection) {
      connectionProviderResolver = NULL_CONNECTION_PROVIDER;
    }

    Optional<ConnectionProvider> connectionProvider = Optional.ofNullable(connectionProviderResolver.resolve(from(event)));
    T configValue = createConfigurationInstance(name, event);

    return new LifecycleAwareConfigurationInstance(name, configurationModel, configValue, createState(connectionProviderResolver),
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
  public ConfigurationInstance createConfiguration(String name,
                                                   ResolverSetResult resolverSetResult,
                                                   Event event,
                                                   Optional<ValueResolver<ConnectionProvider>> connectionProviderResolver)
      throws MuleException {
    T configValue = createConfigurationInstance(name, resolverSetResult);

    Optional<ConnectionProvider> connectionProvider;

    if (requiresConnection && connectionProviderResolver.isPresent()) {
      connectionProvider = ofNullable(connectionProviderResolver.get().resolve(ValueResolvingContext.from(event)));
    } else {
      connectionProvider = empty();
    }

    return new LifecycleAwareConfigurationInstance(name,
                                                   configurationModel,
                                                   configValue,
                                                   createState(connectionProviderResolver.orElse(null)),
                                                   createInterceptors(configurationModel),
                                                   connectionProvider);
  }

  private T createConfigurationInstance(String name, ResolverSetResult resolverSetResult) throws MuleException {
    T config = configurationObjectBuilder.build(resolverSetResult);
    injectRefName(configurationModel, config, name);

    return config;
  }

  private T createConfigurationInstance(String name, Event event) throws MuleException {
    T config = configurationObjectBuilder.build(from(event));
    injectRefName(configurationModel, config, name);

    return config;
  }

  private CheckedFunction<Event, ConfigurationState> createState(ValueResolver<ConnectionProvider> connectionProviderResolver)
      throws MuleException {
    return event -> {
      Map<String, Object> connectionParameters;
      if (connectionProviderResolver instanceof ConnectionProviderValueResolver) {
        connectionParameters =
            (Map<String, Object>) ((ConnectionProviderValueResolver) connectionProviderResolver).getResolverSet()
                .map((CheckedFunction<ResolverSet, Map<String, Object>>) resolverSet -> toMap(resolverSet, event))
                .orElse(emptyMap());
      } else {
        connectionParameters = emptyMap();
      }

      return new ImmutableConfigurationState(toMap(resolverSet, event), connectionParameters);
    };
  }
}
