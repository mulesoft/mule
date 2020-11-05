/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableList;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.assertNotStopping;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.values.ValueResolvingException.UNKNOWN;
import static org.mule.runtime.module.extension.internal.value.ValueProviderUtils.valuesWithClassLoader;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.config.ConfigurationStats;
import org.mule.runtime.extension.api.runtime.config.ExpirableConfigurationProvider;
import org.mule.runtime.extension.api.values.ConfigurationParameterValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.extension.internal.value.ValueProviderMediator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;

/**
 * A {@link ConfigurationProvider} which continuously evaluates the same {@link ResolverSet} and then uses the resulting
 * {@link ResolverSetResult} to build an instance of type {@code T}
 * <p>
 * Although each invocation to {@link #get(Event)} is guaranteed to end up in an invocation to
 * {@link #resolverSet#resolve(Object)}, the resulting {@link ResolverSetResult} might not end up generating a new instance. This
 * is so because {@link ResolverSetResult} instances are put in a cache to guarantee that equivalent evaluations of the
 * {@code resolverSet} return the same instance.
 *
 * @since 4.0.0
 */
public final class DynamicConfigurationProvider extends LifecycleAwareConfigurationProvider
    implements ExpirableConfigurationProvider, ConfigurationParameterValueProvider {

  private static final Logger LOGGER = getLogger(DynamicConfigurationProvider.class);

  private final ConfigurationInstanceFactory configurationInstanceFactory;
  private final ResolverSet resolverSet;
  private final ConnectionProviderValueResolver connectionProviderResolver;
  private final ExpirationPolicy expirationPolicy;

  private final Map<Pair<ResolverSetResult, ResolverSetResult>, ConfigurationInstance> cache = new ConcurrentHashMap<>();
  private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
  private final Lock cacheReadLock = cacheLock.readLock();
  private final Lock cacheWriteLock = cacheLock.writeLock();
  private final ReflectionCache reflectionCache;
  private final ExpressionManager expressionManager;

  /**
   * Creates a new instance
   *
   * @param name                       this provider's name
   * @param extension                  the model that owns the {@code configurationModel}
   * @param config                     the model for the returned configurations
   * @param resolverSet                the {@link ResolverSet} that provides the configuration's parameter values
   * @param connectionProviderResolver a {@link ValueResolver} used to obtain a {@link ConnectionProvider}
   * @param expirationPolicy           the {@link ExpirationPolicy} for the unused instances
   * @param reflectionCache            the {@link ReflectionCache} used to improve reflection lookups performance
   * @param expressionManager          the {@link ExpressionManager} used to create a session used to evaluate the attributes.
   * @param muleContext                the {@link MuleContext} that will own the configuration instances
   */
  public DynamicConfigurationProvider(String name,
                                      ExtensionModel extension,
                                      ConfigurationModel config,
                                      ResolverSet resolverSet,
                                      ConnectionProviderValueResolver connectionProviderResolver,
                                      ExpirationPolicy expirationPolicy,
                                      ReflectionCache reflectionCache,
                                      ExpressionManager expressionManager,
                                      MuleContext muleContext) {
    super(name, extension, config, muleContext);
    this.configurationInstanceFactory =
        new ConfigurationInstanceFactory<>(extension, config, resolverSet, expressionManager, muleContext);
    this.reflectionCache = reflectionCache;
    this.expressionManager = expressionManager;
    this.resolverSet = resolverSet;
    this.connectionProviderResolver = connectionProviderResolver;
    this.expirationPolicy = expirationPolicy;
  }

  /**
   * Evaluates {@link #resolverSet} using the given {@code event} and returns an instance produced with the result. For equivalent
   * {@link ResolverSetResult}s it will return the same instance.
   *
   * @param event the current {@code event}
   * @return the resolved {@link ConfigurationInstance}
   */
  @Override
  public ConfigurationInstance get(Event event) {
    return withContextClassLoader(getExtensionClassLoader(), () -> {
      try (ValueResolvingContext resolvingContext = ValueResolvingContext.builder(((CoreEvent) event))
          .withExpressionManager(expressionManager).build()) {
        ResolverSetResult result = resolverSet.resolve(resolvingContext);
        ResolverSetResult providerResult = null;
        if (connectionProviderResolver.getResolverSet().isPresent()) {
          providerResult = ((ResolverSet) connectionProviderResolver.getResolverSet().get()).resolve(resolvingContext);
        }
        return getConfiguration(new Pair<>(result, providerResult), (CoreEvent) event);
      }
    });
  }

  private ConfigurationInstance getConfiguration(Pair<ResolverSetResult, ResolverSetResult> resolverSetResult, CoreEvent event)
      throws Exception {

    ConfigurationInstance configuration;
    cacheReadLock.lock();
    try {
      configuration = cache.computeIfAbsent(resolverSetResult, (r) -> createConfiguration(r, event));
      updateUsageStatistic(configuration);
      return configuration;
    } catch (WrappingRuntimeException e) {
      throw e.getWrappedException();
    } finally {
      cacheReadLock.unlock();
    }
  }

  private void updateUsageStatistic(ConfigurationInstance configuration) {
    MutableConfigurationStats stats = (MutableConfigurationStats) configuration.getStatistics();
    stats.updateLastUsed();
  }

  private ConfigurationInstance createConfiguration(Pair<ResolverSetResult, ResolverSetResult> values, CoreEvent event) {
    assertNotStopping(muleContext, "Mule is shutting down... Cannot create new dynamic configurations");

    ConfigurationInstance configuration;
    ResolverSetResult connectionProviderValues = values.getSecond();
    try {
      if (connectionProviderValues != null) {
        configuration = configurationInstanceFactory.createConfiguration(getName(),
                                                                         values.getFirst(),
                                                                         event,
                                                                         connectionProviderResolver,
                                                                         connectionProviderValues);
      } else {
        configuration = configurationInstanceFactory.createConfiguration(getName(),
                                                                         values.getFirst(),
                                                                         event,
                                                                         ofNullable(connectionProviderResolver));
      }
      registerConfiguration(configuration);
      return configuration;
    } catch (MuleException e) {
      throw new WrappingRuntimeException(e);
    }
  }

  @Override
  protected void registerConfiguration(ConfigurationInstance configuration) {
    try {
      withContextClassLoader(getExtensionClassLoader(), () -> {
        if (lifecycleManager.isPhaseComplete(Initialisable.PHASE_NAME)) {
          try {
            initialiseIfNeeded(configuration, true, muleContext);
          } catch (Exception e) {
            disposeIfNeeded(configuration, LOGGER);
            throw e;
          }
        }

        if (lifecycleManager.isPhaseComplete(Startable.PHASE_NAME)) {
          try {
            startConfig(configuration);
          } catch (Exception e) {
            try {
              stopIfNeeded(configuration);
            } catch (Exception ex) {
              // Ignore and continue with the disposal
              LOGGER.warn("Exception while stopping " + configuration.toString(), e);
            }
            disposeIfNeeded(configuration, LOGGER);
            throw e;
          }
        }

        return null;
      });
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not register configuration of key " + getName()), e);
    }

    super.registerConfiguration(configuration);
  }

  @Override
  public List<ConfigurationInstance> getExpired() {
    cacheWriteLock.lock();
    try {
      return cache.entrySet().stream().filter(entry -> isExpired(entry.getValue())).map(entry -> {
        cache.remove(entry.getKey());
        unRegisterConfiguration(entry.getValue());
        return entry.getValue();
      }).collect(toImmutableList());
    } finally {
      cacheWriteLock.unlock();
    }
  }

  private boolean isExpired(ConfigurationInstance configuration) {
    ConfigurationStats stats = configuration.getStatistics();
    return stats.getActiveComponents() == 0 && expirationPolicy.isExpired(stats.getLastUsedMillis(), MILLISECONDS);
  }

  @Override
  protected void doInitialise() {
    try {
      initialiseIfNeeded(resolverSet, muleContext);
      initialiseIfNeeded(connectionProviderResolver, muleContext);
    } catch (InitialisationException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public void start() throws MuleException {
    super.start();
    startIfNeeded(connectionProviderResolver);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Value> getConfigValues(String parameterName) throws ValueResolvingException {
    return valuesWithClassLoader(() -> new ValueProviderMediator<>(getConfigurationModel(), () -> muleContext,
                                                                   () -> reflectionCache)
                                                                       .getValues(parameterName,
                                                                                  new ResolverSetBasedParameterResolver(resolverSet,
                                                                                                                        getConfigurationModel(),
                                                                                                                        reflectionCache,
                                                                                                                        expressionManager)),
                                 getExtensionModel());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Value> getConnectionValues(String parameterName) throws ValueResolvingException {
    return valuesWithClassLoader(() -> {
      ConnectionProviderModel connectionProviderModel = getConnectionProviderModel()
          .orElseThrow(() -> new ValueResolvingException(
                                                         "Internal Error. Unable to resolve values because the service is unable to get the connection model",
                                                         UNKNOWN));
      ResolverSet resolverSet = ((Optional<ResolverSet>) connectionProviderResolver.getResolverSet())
          .orElseThrow(() -> new ValueResolvingException(
                                                         "Internal Error. Unable to resolve values because of the service is unable to retrieve connection parameters",
                                                         UNKNOWN));

      return new ValueProviderMediator<>(connectionProviderModel,
                                         () -> muleContext,
                                         () -> reflectionCache)
                                             .getValues(parameterName,
                                                        new ResolverSetBasedParameterResolver(resolverSet,
                                                                                              connectionProviderModel,
                                                                                              reflectionCache,
                                                                                              expressionManager));
    }, getExtensionModel());
  }

  private Optional<ConnectionProviderModel> getConnectionProviderModel() {
    return this.connectionProviderResolver.getObjectBuilder()
        .filter(ob -> ob instanceof ConnectionProviderObjectBuilder)
        .map(ob -> ((ConnectionProviderObjectBuilder) ob).providerModel);
  }

  /**
   * Used to preserve exception throwing behaviour in {@link #getConfiguration} as the method {@link #createConfiguration} can no longer throw
   * checked exception as it is used inside {@link Map#computeIfAbsent}.
   */
  private static class WrappingRuntimeException extends RuntimeException {

    private final Exception wrappedException;

    public WrappingRuntimeException(Exception e) {
      super(e);
      this.wrappedException = e;
    }

    public Exception getWrappedException() {
      return wrappedException;
    }
  }
}
