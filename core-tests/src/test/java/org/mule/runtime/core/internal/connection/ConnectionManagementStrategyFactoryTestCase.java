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

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsSame.sameInstance;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mockito.quality.Strictness.LENIENT;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.sdk.api.connectivity.XATransactionalConnection;
import org.mule.sdk.api.connectivity.XATransactionalConnectionProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
class ConnectionManagementStrategyFactoryTestCase extends AbstractMuleTestCase {

  private ConnectionManagementStrategyFactory connMgmtStrategyFactory;
  private ConnectionManagementStrategyFactory noXaSupportConnMgmtStrategyFactory;

  private PoolingProfile poolingProfile;

  @Mock
  private XAConnectionManagementStrategyFactory xaConnectionManagementStrategyFactory;
  @Mock
  private ConnectionManagementStrategy managePooledForXa;
  @Mock
  private ConnectionManagementStrategy manageForXa;

  @Mock
  private LifecycleState deploymentLifecycleState;

  @BeforeEach
  void setUp() throws ConnectionException {
    when(xaConnectionManagementStrategyFactory.managePooledForXa(any(), any()))
        .thenReturn(managePooledForXa);
    when(xaConnectionManagementStrategyFactory.manageForXa(any(), any(), any()))
        .thenReturn(manageForXa);

    connMgmtStrategyFactory = new ConnectionManagementStrategyFactory(null, deploymentLifecycleState,
                                                                      of(xaConnectionManagementStrategyFactory));
    noXaSupportConnMgmtStrategyFactory = new ConnectionManagementStrategyFactory(null, deploymentLifecycleState,
                                                                                 empty());

    poolingProfile = new PoolingProfile();
  }

  @Test
  void noManagement() {
    final var connectionProvider = mock(ConnectionProviderWrapper.class);
    when(connectionProvider.getConnectionManagementType()).thenReturn(null);

    final FeatureFlaggingService featureFlaggingService = getFeatureFlaggingService();
    assertThrows(IllegalArgumentException.class,
                 () -> connMgmtStrategyFactory.getStrategy(connectionProvider, featureFlaggingService));
  }

  @Test
  void pooling() throws ConnectionException {
    final var poolingConnectionProvider = mock(ConnectionProviderWrapper.class);
    configureAsPooling(poolingConnectionProvider);

    final var strategy = connMgmtStrategyFactory.getStrategy(poolingConnectionProvider, getFeatureFlaggingService());
    assertThat(strategy, instanceOf(PoolingConnectionManagementStrategy.class));
    assertThat(((PoolingConnectionManagementStrategy) strategy).getPoolingProfile(), sameInstance(poolingProfile));

    verify(xaConnectionManagementStrategyFactory, never()).manageForXa(any(), any(), any());
    verify(xaConnectionManagementStrategyFactory, never()).managePooledForXa(any(), any());
    assertConnectionProvided(poolingConnectionProvider, strategy);
  }

  @Test
  void poolingDisabled() throws ConnectionException {
    poolingProfile.setDisabled(true);

    final var poolingConnectionProvider = mock(ConnectionProviderWrapper.class);
    configureAsPooling(poolingConnectionProvider);

    final var strategy = connMgmtStrategyFactory.getStrategy(poolingConnectionProvider, getFeatureFlaggingService());
    assertThat(strategy, instanceOf(NullConnectionManagementStrategy.class));

    verify(xaConnectionManagementStrategyFactory, never()).manageForXa(any(), any(), any());
    verify(xaConnectionManagementStrategyFactory, never()).managePooledForXa(any(), any());
    assertConnectionProvided(poolingConnectionProvider, strategy);
  }

  @Test
  void cached() throws ConnectionException {
    final var cachedConnectionProvider = mock(ConnectionProviderWrapper.class);
    when(cachedConnectionProvider.getConnectionManagementType()).thenReturn(CACHED);

    when(deploymentLifecycleState.isStopped()).thenReturn(false);
    when(deploymentLifecycleState.isStopping()).thenReturn(false);

    final var strategy = connMgmtStrategyFactory.getStrategy(cachedConnectionProvider, getFeatureFlaggingService());
    assertThat(strategy, instanceOf(CachedConnectionManagementStrategy.class));

    verify(xaConnectionManagementStrategyFactory, never()).manageForXa(any(), any(), any());
    verify(xaConnectionManagementStrategyFactory, never()).managePooledForXa(any(), any());
    assertConnectionProvided(cachedConnectionProvider, strategy);
  }

  @Test
  void basic() throws ConnectionException {
    final var connectionProvider = mock(ConnectionProviderWrapper.class);
    when(connectionProvider.getConnectionManagementType()).thenReturn(NONE);

    final var strategy = connMgmtStrategyFactory.getStrategy(connectionProvider, getFeatureFlaggingService());
    assertThat(strategy, instanceOf(NullConnectionManagementStrategy.class));

    verify(xaConnectionManagementStrategyFactory, never()).manageForXa(any(), any(), any());
    verify(xaConnectionManagementStrategyFactory, never()).managePooledForXa(any(), any());
    assertConnectionProvided(connectionProvider, strategy);
  }

  @Test
  void poolingTx() throws ConnectionException {
    final var poolingConnectionProvider = mock(ConnectionProviderWrapper.class);
    configureAsPooling(poolingConnectionProvider);

    final var strategy = connMgmtStrategyFactory.getStrategy(poolingConnectionProvider, getFeatureFlaggingService());
    assertThat(strategy, instanceOf(PoolingConnectionManagementStrategy.class));
    assertThat(((PoolingConnectionManagementStrategy) strategy).getPoolingProfile(), sameInstance(poolingProfile));

    verify(xaConnectionManagementStrategyFactory, never()).manageForXa(any(), any(), any());
    verify(xaConnectionManagementStrategyFactory, never()).managePooledForXa(any(), any());
    assertConnectionProvided(poolingConnectionProvider, strategy);
  }

  @Test
  void cachedTx() throws ConnectionException {
    final var cachedConnectionProvider = mock(ConnectionProviderWrapper.class);
    when(cachedConnectionProvider.getConnectionManagementType()).thenReturn(CACHED);

    when(deploymentLifecycleState.isStopped()).thenReturn(false);
    when(deploymentLifecycleState.isStopping()).thenReturn(false);

    final var strategy = connMgmtStrategyFactory.getStrategy(cachedConnectionProvider, getFeatureFlaggingService());
    assertThat(strategy, instanceOf(CachedConnectionManagementStrategy.class));

    verify(xaConnectionManagementStrategyFactory, never()).manageForXa(any(), any(), any());
    verify(xaConnectionManagementStrategyFactory, never()).managePooledForXa(any(), any());
    assertConnectionProvided(cachedConnectionProvider, strategy);
  }

  @Test
  void basicTx() throws ConnectionException {
    final var connectionProvider = mock(ConnectionProviderWrapper.class);
    when(connectionProvider.getConnectionManagementType()).thenReturn(NONE);

    final var strategy = connMgmtStrategyFactory.getStrategy(connectionProvider, getFeatureFlaggingService());
    assertThat(strategy, instanceOf(NullConnectionManagementStrategy.class));

    verify(xaConnectionManagementStrategyFactory, never()).manageForXa(any(), any(), any());
    verify(xaConnectionManagementStrategyFactory, never()).managePooledForXa(any(), any());
    assertConnectionProvided(connectionProvider, strategy);
  }

  @Test
  void poolingXaTx() throws ConnectionException {
    final var poolingConnectionProvider = createXaConnectionProvider();
    configureAsPooling(poolingConnectionProvider);

    final var strategy = connMgmtStrategyFactory.getStrategy(poolingConnectionProvider, getFeatureFlaggingService());
    assertThat(strategy, sameInstance(managePooledForXa));

    verify(((XATransactionalConnectionProvider) poolingConnectionProvider), never()).getXaPoolingProfile();
    assertPooledXaManaged(xaConnectionManagementStrategyFactory, poolingProfile);
  }

  @Test
  void cachedXaTx() throws ConnectionException {
    final var cachedConnectionProvider = createXaConnectionProvider();
    when(cachedConnectionProvider.getConnectionManagementType()).thenReturn(CACHED);

    when(deploymentLifecycleState.isStopped()).thenReturn(false);
    when(deploymentLifecycleState.isStopping()).thenReturn(false);

    final var strategy = connMgmtStrategyFactory.getStrategy(cachedConnectionProvider, getFeatureFlaggingService());
    assertThat(strategy, sameInstance(manageForXa));

    verify(((XATransactionalConnectionProvider) cachedConnectionProvider)).getXaPoolingProfile();
    assertXaManaged(xaConnectionManagementStrategyFactory, poolingProfile);
  }

  @Test
  void basicXaTx() throws ConnectionException {
    final var connectionProvider = createXaConnectionProvider();
    when(connectionProvider.getConnectionManagementType()).thenReturn(NONE);

    final var strategy = connMgmtStrategyFactory.getStrategy(connectionProvider, getFeatureFlaggingService());
    assertThat(strategy, sameInstance(manageForXa));

    verify(((XATransactionalConnectionProvider) connectionProvider)).getXaPoolingProfile();
    assertXaManaged(xaConnectionManagementStrategyFactory, poolingProfile);
  }

  private static void assertPooledXaManaged(XAConnectionManagementStrategyFactory xaConnectionManagementStrategyFactory,
                                            final PoolingProfile poolingProfile) {
    verify(xaConnectionManagementStrategyFactory).managePooledForXa(argThat(mgmtStrategy -> {
      assertThat(mgmtStrategy, instanceOf(PoolingConnectionManagementStrategy.class));
      assertThat(((PoolingConnectionManagementStrategy) mgmtStrategy).getPoolingProfile(), sameInstance(poolingProfile));

      return true;
    }),
                                                                    any());
  }

  private static void assertXaManaged(XAConnectionManagementStrategyFactory xaConnectionManagementStrategyFactory,
                                      final PoolingProfile poolingProfile) {
    verify(xaConnectionManagementStrategyFactory).manageForXa(any(),
                                                              argThat(xaPoolingProfile -> xaPoolingProfile == poolingProfile),
                                                              any());
  }

  @Test
  void poolingXaTxNoXaSupport() throws ConnectionException {
    final var poolingConnectionProvider = createXaConnectionProvider();
    configureAsPooling(poolingConnectionProvider);

    final var strategy = noXaSupportConnMgmtStrategyFactory.getStrategy(poolingConnectionProvider, getFeatureFlaggingService());
    assertThat(strategy, instanceOf(PoolingConnectionManagementStrategy.class));

    verify(((XATransactionalConnectionProvider) poolingConnectionProvider), never()).getXaPoolingProfile();
    assertConnectionProvided(poolingConnectionProvider, strategy);
  }

  @Test
  void cachedXaTxNoXaSupport() throws ConnectionException {
    final var cachedConnectionProvider = createXaConnectionProvider();
    when(cachedConnectionProvider.getConnectionManagementType()).thenReturn(CACHED);

    when(deploymentLifecycleState.isStopped()).thenReturn(false);
    when(deploymentLifecycleState.isStopping()).thenReturn(false);

    final var strategy = noXaSupportConnMgmtStrategyFactory.getStrategy(cachedConnectionProvider, getFeatureFlaggingService());
    assertThat(strategy, instanceOf(CachedConnectionManagementStrategy.class));

    verify(((XATransactionalConnectionProvider) cachedConnectionProvider), never()).getXaPoolingProfile();
    assertConnectionProvided(cachedConnectionProvider, strategy);
  }

  @Test
  void basicXaTxNoXaSupport() throws ConnectionException {
    final var connectionProvider = createXaConnectionProvider();
    when(connectionProvider.getConnectionManagementType()).thenReturn(NONE);

    final var strategy = noXaSupportConnMgmtStrategyFactory.getStrategy(connectionProvider, getFeatureFlaggingService());
    assertThat(strategy, instanceOf(NullConnectionManagementStrategy.class));

    verify(((XATransactionalConnectionProvider) connectionProvider), never()).getXaPoolingProfile();
    assertConnectionProvided(connectionProvider, strategy);
  }

  private ConnectionProviderWrapper createXaConnectionProvider() {
    final var connectionProvider = mock(ConnectionProviderWrapper.class,
                                        withSettings().extraInterfaces(XATransactionalConnectionProvider.class,
                                                                       org.mule.runtime.core.internal.connection.adapter.XATransactionalConnectionProvider.class));
    when(connectionProvider.supportsXa()).thenReturn(true);
    when(((XATransactionalConnectionProvider) connectionProvider).getXaPoolingProfile()).thenReturn(poolingProfile);

    return connectionProvider;
  }

  private void configureAsPooling(final ConnectionProviderWrapper poolingConnectionProvider) {
    final var delegatePoolingConnectionProvider = mock(org.mule.runtime.api.connection.PoolingConnectionProvider.class);
    when(poolingConnectionProvider.getDelegate()).thenReturn(delegatePoolingConnectionProvider);
    when(poolingConnectionProvider.getPoolingProfile()).thenReturn(of(poolingProfile));

    when(poolingConnectionProvider.getConnectionManagementType()).thenReturn(POOLING);
  }

  private static void assertConnectionProvided(final ConnectionProviderWrapper connectionProvider,
                                               final ConnectionManagementStrategy strategy)
      throws ConnectionException {
    final var xaConnection = mock(XATransactionalConnection.class);
    when(connectionProvider.connect()).thenReturn(xaConnection);
    assertThat(strategy.getConnectionHandler().getConnection(), sameInstance(xaConnection));
  }
}
