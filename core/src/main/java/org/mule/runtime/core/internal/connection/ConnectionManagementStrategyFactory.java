/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.CACHED;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.NONE;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.POOLING;
import static org.mule.runtime.core.api.connection.util.ConnectionProviderUtils.unwrapProviderWrapper;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.api.connection.PoolingListener;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.internal.connection.adapter.XATransactionalConnectionProvider;
import org.mule.sdk.api.connectivity.TransactionalConnection;

import java.util.Optional;

/**
 * Creates instances of {@link ConnectionManagementStrategy}
 *
 * @since 4.0
 */
final class ConnectionManagementStrategyFactory {

  private final PoolingProfile defaultPoolingProfile;
  private final LifecycleState deploymentLifecycleState;
  private final Optional<XAConnectionManagementStrategyFactory> xaConnectionManagementStrategyFactory;

  /**
   * Creates a new instance
   *
   * @param defaultPoolingProfile the {@link PoolingProfile} that will be used to configure the pool of connections
   * @param muleContext           the owning {@link MuleContext}
   */
  ConnectionManagementStrategyFactory(PoolingProfile defaultPoolingProfile, LifecycleState deploymentLifecycleState,
                                      Optional<XAConnectionManagementStrategyFactory> xaConnectionManagementStrategyFactory) {
    this.defaultPoolingProfile = defaultPoolingProfile;
    this.deploymentLifecycleState = deploymentLifecycleState;
    this.xaConnectionManagementStrategyFactory = xaConnectionManagementStrategyFactory;
  }

  /**
   * Returns the management strategy that should be used for the given {@code connectionProvider}
   *
   * @param <C>                    the generic type of the connections to be managed
   * @param connectionProvider     a {@link ConnectionProvider}
   * @param featureFlaggingService the {@link FeatureFlaggingService}
   * @return a {@link ConnectionManagementStrategy}
   */
  public <C> ConnectionManagementStrategy<C> getStrategy(ConnectionProvider<C> connectionProvider,
                                                         FeatureFlaggingService featureFlaggingService) {
    ConnectionManagementType managementType = getManagementType(connectionProvider);

    ConnectionManagementStrategy<C> managementStrategy = null;

    if (managementType == POOLING) {
      managementStrategy = pooling(connectionProvider, featureFlaggingService);
    }

    if (unwrapProviderWrapper(connectionProvider,
                              XATransactionalConnectionProvider.class) instanceof XATransactionalConnectionProvider<?> xaTxConnectionProvider) {
      return handleForXa((ConnectionProvider) connectionProvider,
                         managementType,
                         (ConnectionManagementStrategy) managementStrategy,
                         xaTxConnectionProvider);
    } else if (managementStrategy != null) {
      return managementStrategy;
    } else if (managementType == CACHED) {
      return cached(connectionProvider);
    } else if (managementType == NONE) {
      return withoutManagement(connectionProvider);
    } else {
      throw new IllegalArgumentException("Unknown management type: " + managementType);
    }
  }

  private <C extends TransactionalConnection> ConnectionManagementStrategy<C> handleForXa(ConnectionProvider<C> connectionProvider,
                                                                                          ConnectionManagementType managementType,
                                                                                          ConnectionManagementStrategy<C> poolingManagementStrategy,
                                                                                          XATransactionalConnectionProvider<C> xaTXConnProvider) {
    if (poolingManagementStrategy != null) {
      // this will even be the case it the connection provider is pooled, but pooling is disabled. In this case there won´t be an
      // XA pool either, as it was explicitly configured that way.
      return xaConnectionManagementStrategyFactory
          .map(f -> f.managePooledForXa(poolingManagementStrategy,
                                        connectionProvider))
          .orElse(poolingManagementStrategy);
    } else {
      return xaConnectionManagementStrategyFactory
          .map(f -> f.manageForXa(withoutManagement(connectionProvider),
                                  xaTXConnProvider.getXaPoolingProfile(),
                                  connectionProvider))
          .orElseGet(() -> getNonPoolingStrategy(connectionProvider, managementType));
    }
  }

  private <C> ConnectionManagementStrategy<C> getNonPoolingStrategy(ConnectionProvider<C> connectionProvider,
                                                                    ConnectionManagementType managementType) {
    if (managementType == CACHED) {
      return cached(connectionProvider);
    } else if (managementType == NONE) {
      return withoutManagement(connectionProvider);
    } else {
      throw new IllegalArgumentException("Unknown management type: " + managementType);
    }
  }

  private <C> ConnectionManagementStrategy<C> cached(ConnectionProvider<C> connectionProvider) {
    return new CachedConnectionManagementStrategy<>(connectionProvider, deploymentLifecycleState);
  }

  private <C> ConnectionManagementStrategy<C> withoutManagement(ConnectionProvider<C> connectionProvider) {
    return new NullConnectionManagementStrategy<>(connectionProvider);
  }

  private <C> ConnectionManagementStrategy<C> pooling(ConnectionProvider<C> connectionProvider,
                                                      FeatureFlaggingService featureFlaggingService) {
    PoolingProfile poolingProfile = defaultPoolingProfile;
    if (connectionProvider instanceof ConnectionProviderWrapper<C> cpWrapper) {
      poolingProfile = cpWrapper.getPoolingProfile().orElse(poolingProfile);
    }

    return pooling(poolingProfile, connectionProvider,
                   (PoolingListener<C>) unwrapProviderWrapper(connectionProvider,
                                                              PoolingConnectionProvider.class),
                   featureFlaggingService);
  }

  private <C> ConnectionManagementStrategy<C> pooling(PoolingProfile poolingProfile,
                                                      ConnectionProvider<C> connectionProvider,
                                                      PoolingListener<C> poolingListener,
                                                      FeatureFlaggingService featureFlaggingService) {
    String ownerConfigName = "";
    if (connectionProvider instanceof ConnectionProviderWrapper<C> cpWrapper) {
      ownerConfigName = cpWrapper.getOwnerConfigName().orElse(ownerConfigName);
    }

    return poolingProfile.isDisabled()
        ? withoutManagement(connectionProvider)
        : new PoolingConnectionManagementStrategy<>(connectionProvider, poolingProfile,
                                                    poolingListener,
                                                    ownerConfigName, featureFlaggingService);
  }

  private <C> ConnectionManagementType getManagementType(ConnectionProvider<C> connectionProvider) {
    ConnectionManagementType type = NONE;

    if (connectionProvider instanceof ConnectionProviderWrapper<C> cpWrapper) {
      return cpWrapper.getConnectionManagementType();
    }

    if (connectionProvider instanceof PoolingConnectionProvider) {
      type = POOLING;
    } else if (connectionProvider instanceof CachedConnectionProvider) {
      type = CACHED;
    }

    return type;
  }
}
