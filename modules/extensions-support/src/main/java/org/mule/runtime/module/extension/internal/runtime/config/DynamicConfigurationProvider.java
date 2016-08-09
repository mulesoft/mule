/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.ConfigurationStats;
import org.mule.runtime.extension.api.runtime.ExpirableConfigurationProvider;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A {@link ConfigurationProvider} which continuously evaluates the same {@link ResolverSet} and then uses the resulting
 * {@link ResolverSetResult} to build an instance of type {@code T}
 * <p>
 * Although each invocation to {@link #get(Object)} is guaranteed to end up in an invocation to
 * {@link #resolverSet#resolve(MuleEvent)}, the resulting {@link ResolverSetResult} might not end up generating a new instance.
 * This is so because {@link ResolverSetResult} instances are put in a cache to guarantee that equivalent evaluations of the
 * {@code resolverSet} return the same instance.
 *
 * @param <T> the generic type of the provided {@link ConfigurationInstance}
 * @since 4.0.0
 */
public final class DynamicConfigurationProvider<T> extends LifecycleAwareConfigurationProvider<T>
    implements ExpirableConfigurationProvider<T> {

  private final ConfigurationInstanceFactory<T> configurationInstanceFactory;
  private final ResolverSet resolverSet;
  private final ValueResolver<ConnectionProvider> connectionProviderResolver;
  private final ExpirationPolicy expirationPolicy;

  private final Map<ResolverSetResult, ConfigurationInstance<T>> cache = new ConcurrentHashMap<>();
  private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
  private final Lock cacheReadLock = cacheLock.readLock();
  private final Lock cacheWriteLock = cacheLock.writeLock();

  /**
   * Creates a new instance
   *
   * @param name this provider's name
   * @param configurationModel the model for the returned configurations
   * @param resolverSet the {@link ResolverSet} that provides the configuration's parameter values
   * @param connectionProviderResolver a {@link ValueResolver} used to obtain a {@link ConnectionProvider}
   * @param expirationPolicy the {@link ExpirationPolicy} for the unused instances
   */
  public DynamicConfigurationProvider(String name, RuntimeConfigurationModel configurationModel, ResolverSet resolverSet,
                                      ValueResolver<ConnectionProvider> connectionProviderResolver,
                                      ExpirationPolicy expirationPolicy) {
    super(name, configurationModel);
    configurationInstanceFactory = new ConfigurationInstanceFactory<>(configurationModel, resolverSet);
    this.resolverSet = resolverSet;
    this.connectionProviderResolver = connectionProviderResolver;
    this.expirationPolicy = expirationPolicy;
  }

  /**
   * Evaluates {@link #resolverSet} using the given {@code muleEvent} and returns an instance produced with the result. For
   * equivalent {@link ResolverSetResult}s it will return the same instance.
   *
   * @param muleEvent the current {@link MuleEvent}
   * @return the resolved {@link ConfigurationInstance}
   */
  @Override
  public ConfigurationInstance<T> get(Object muleEvent) {
    return withContextClassLoader(getExtensionClassLoader(), () -> {
      ResolverSetResult result = resolverSet.resolve((MuleEvent) muleEvent);
      return getConfiguration(result, (MuleEvent) muleEvent);
    });
  }

  private ConfigurationInstance<T> getConfiguration(ResolverSetResult resolverSetResult, MuleEvent event) throws Exception {
    ConfigurationInstance<T> configuration;
    cacheReadLock.lock();
    try {
      configuration = cache.get(resolverSetResult);
      if (configuration != null) {
        // important to account between the boundaries of the lock to prevent race condition
        updateUsageStatistic(configuration);
        return configuration;
      }
    } finally {
      cacheReadLock.unlock();
    }

    cacheWriteLock.lock();
    try {
      // re-check in case some other thread beat us to it...
      configuration = cache.get(resolverSetResult);
      if (configuration == null) {
        configuration = createConfiguration(resolverSetResult, event);
        cache.put(resolverSetResult, configuration);
      }

      // accounting here for the same reasons as above
      updateUsageStatistic(configuration);
      return configuration;
    } finally {
      cacheWriteLock.unlock();
    }
  }

  private void updateUsageStatistic(ConfigurationInstance<T> configuration) {
    MutableConfigurationStats stats = (MutableConfigurationStats) configuration.getStatistics();
    stats.updateLastUsed();
  }

  private ConfigurationInstance<T> createConfiguration(ResolverSetResult result, MuleEvent event) throws MuleException {
    ConfigurationInstance<T> configuration = configurationInstanceFactory
        .createConfiguration(getName(), result, Optional.ofNullable(connectionProviderResolver.resolve(event)));

    registerConfiguration(configuration);

    return configuration;
  }

  @Override
  protected void registerConfiguration(ConfigurationInstance<T> configuration) {
    try {
      withContextClassLoader(getExtensionClassLoader(), () -> {
        if (lifecycleManager.isPhaseComplete(Initialisable.PHASE_NAME)) {
          initialiseIfNeeded(configuration, true, muleContext);
        }

        if (lifecycleManager.isPhaseComplete(Startable.PHASE_NAME)) {
          startConfig(configuration);
        }

        return null;
      });
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not register configuration of key " + getName()), e);
    }

    super.registerConfiguration(configuration);
  }

  @Override
  public List<ConfigurationInstance<T>> getExpired() {
    cacheWriteLock.lock();
    try {
      return cache.entrySet().stream().filter(entry -> isExpired(entry.getValue())).map(entry -> {
        cache.remove(entry.getKey());
        return entry.getValue();
      }).collect(new ImmutableListCollector<>());
    } finally {
      cacheWriteLock.unlock();
    }
  }

  private boolean isExpired(ConfigurationInstance<T> configuration) {
    ConfigurationStats stats = configuration.getStatistics();
    return stats.getInflightOperations() == 0 && expirationPolicy.isExpired(stats.getLastUsedMillis(), TimeUnit.MILLISECONDS);
  }
}
