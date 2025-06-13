/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Objects.requireNonNull;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.retry.ReconnectionConfig;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.registry.InjectionTargetDecorator;
import org.mule.runtime.core.internal.util.InjectionUtils;

import java.util.Optional;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;

/**
 * Connection handling utilities
 *
 * @since 4.3.0
 */
public final class ConnectionUtils {

  private ConnectionUtils() {}

  /**
   * Returns the {@link RetryPolicyTemplate} defined in the {@code reconnectionConfig}. If none was specified or the
   * {@code reconnectionConfig} is empty, then a {@link NoRetryPolicyTemplate} is created and returned
   *
   * @param reconnectionConfig an optional {@link ReconnectionConfig}
   * @return a {@link RetryPolicyTemplate}
   */
  public static RetryPolicyTemplate getRetryPolicyTemplate(Optional<ReconnectionConfig> reconnectionConfig) {
    return reconnectionConfig
        .map(ReconnectionConfig::getRetryPolicyTemplate)
        .orElseGet(NoRetryPolicyTemplate::new);
  }

  /**
   * Invokes the {@link ConnectionProvider#connect()} method on the given {@code delegate}.
   *
   * @param delegate a {@link ConnectionProvider}
   * @param <C>      the generic type of the returned connection
   * @return a connection
   * @throws ConnectionException
   */
  public static <C> C connect(ConnectionProvider<C> delegate) throws ConnectionException {
    try {
      return requireNonNull(delegate.connect(), delegate.getClass().getName() + "#connect() returned null.");
    } catch (ConnectionException ce) {
      throw ce;
    } catch (Exception e) {
      throw new ConnectionException(e);
    }
  }

  /**
   * Use this method to obtain the real connection provider in case the supplied {@code connectionProvider} is a
   * {@link ConnectionProviderWrapper}
   *
   * @param connectionProvider a connection provider
   * @param <C>                the connection generic type
   * @return the unwrapped provider
   * @since 4.5.0
   */
  public static <C> ConnectionProvider<C> unwrap(ConnectionProvider<C> connectionProvider) {
    while (connectionProvider instanceof ConnectionProviderWrapper) {
      connectionProvider = ((ConnectionProviderWrapper<C>) connectionProvider).getDelegate();
    }

    return connectionProvider;
  }

  /**
   * When performing injecting dependencies or parameter values, the target {@code connectionProvider} might be wrapped in a
   * {@link ConnectionProviderWrapper} or other types of {@link InjectionTargetDecorator}. Use this method to obtain the actual
   * instance in which injection needs to happen so that the adapters don't hide the injection targets during introspection.
   *
   * @param connectionProvider the target connection provider
   * @param <C>                the connection generic type
   * @return the real injection target
   * @since 4.5.0
   */
  public static <C> Object getInjectionTarget(ConnectionProvider<C> connectionProvider) {
    return InjectionUtils.getInjectionTarget(unwrap(connectionProvider));
  }

  public static <C> void logPoolStatus(Logger logger, GenericObjectPool<C> pool, String poolId) {
    logger.atDebug()
        .setMessage("Status for pool {}: {} connections are active out of {} max active limit, {} connections are idle out of {} max idle limit")
        .addArgument(poolId)
        .addArgument(pool.getNumActive())
        .addArgument(() -> pool.getMaxTotal() < 0 || pool.getMaxTotal() == MAX_VALUE
            ? "unlimited"
            : String.valueOf(pool.getMaxTotal()))
        .addArgument(pool.getNumIdle())
        .addArgument(() -> pool.getMaxIdle() < 0
            ? "unlimited"
            : String.valueOf(pool.getMaxIdle()))
        .log();
  }
}
