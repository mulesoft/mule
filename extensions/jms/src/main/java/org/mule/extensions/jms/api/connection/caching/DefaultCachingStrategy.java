/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection.caching;

import static java.util.Optional.of;
import org.mule.extensions.jms.internal.connection.JmsCachingConnectionFactory;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.XAConnectionFactory;

import org.springframework.jms.connection.CachingConnectionFactory;

/**
 * Default implementation of {@link CachingConfiguration} that not only enables caching
 * but also provides default values for all the configurable parameters
 *
 * @since 4.0
 */
@Alias("default-caching")
public final class DefaultCachingStrategy implements CachingStrategy, CachingConfiguration {

  /**
   * Amount of {@link Session}s to cache
   */
  @Parameter
  @Optional(defaultValue = "1")
  int sessionCacheSize;

  /**
   * {@code true} if the {@link ConnectionFactory} should cache the {@link MessageProducer}s
   */
  @Parameter
  @Alias("cacheProducers")
  @Optional(defaultValue = "true")
  boolean producersCache;

  /**
   * {@code true} if the {@link ConnectionFactory} should cache the {@link MessageConsumer}s
   */
  @Parameter
  @Alias("cacheConsumers")
  @Optional(defaultValue = "true")
  boolean consumersCache;


  /**
   * @return the {@code sessionCacheSize}
   */
  public int getSessionCacheSize() {
    return sessionCacheSize;
  }

  /**
   * @return {@code true} if {@link MessageProducer}s should be cached
   */
  public boolean isProducersCache() {
    return producersCache;
  }

  /**
   * @return {@code true} if {@link MessageConsumer}s should be cached
   */
  public boolean isConsumersCache() {
    return consumersCache;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean appliesTo(ConnectionFactory target) {
    return !(target instanceof XAConnectionFactory
        || target instanceof JmsCachingConnectionFactory
        || target instanceof CachingConnectionFactory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public java.util.Optional<CachingConfiguration> strategyConfiguration() {
    return of(this);
  }
}
