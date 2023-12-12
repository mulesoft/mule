/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;

import java.util.Optional;

import org.apache.commons.pool.impl.GenericObjectPool;
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
      return delegate.connect();
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
   */
  public static <C> Object getInjectionTarget(ConnectionProvider<C> connectionProvider) {
    return InjectionUtils.getInjectionTarget(unwrap(connectionProvider));
  }

  public static <C> void logPoolStatus(Logger logger, GenericObjectPool<C> pool, String poolId) {
    if (logger.isDebugEnabled()) {
      String maxActive = pool.getMaxActive() < 0 ? "unlimited" : String.valueOf(pool.getMaxActive());
      String maxIdle = pool.getMaxIdle() < 0 ? "unlimited" : String.valueOf(pool.getMaxIdle());
      logger
          .debug("Status for pool {}: {} connections are active out of {} max active limit, {} connections are idle out of {} max idle limit",
                 poolId, pool.getNumActive(), maxActive, pool.getNumIdle(), maxIdle);
    }
  }

  public interface InjectionTargetDecorator<T> {

    /**
     * @return the decorated instance
     */
    T getDelegate();
  }

  /**
   * Utilities for dependency and values injection
   */
  public static final class InjectionUtils {

    private InjectionUtils() {}

    /**
     * Recursively tests the given {@code object} to be an instance of {@link InjectionTargetDecorator} and returns the actual
     * instance in which injection is to happen
     *
     * @param target the injection target
     * @param <T>    the target's generic type
     * @return the actual injection target
     */
    public static <T> T getInjectionTarget(T target) {
      while (target instanceof InjectionTargetDecorator) {
        target = ((InjectionTargetDecorator<T>) target).getDelegate();
      }

      return target;
    }
  }
}
