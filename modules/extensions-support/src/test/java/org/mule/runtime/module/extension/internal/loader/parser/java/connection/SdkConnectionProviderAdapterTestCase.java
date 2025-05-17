/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.connection;

import static org.mule.runtime.module.extension.internal.loader.parser.java.connection.SdkConnectionProviderAdapter.from;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.sdk.api.connectivity.CachedConnectionProvider;
import org.mule.sdk.api.connectivity.ConnectionProvider;
import org.mule.sdk.api.connectivity.PoolingConnectionProvider;
import org.mule.sdk.api.connectivity.XATransactionalConnectionProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.jupiter.api.Test;

class SdkConnectionProviderAdapterTestCase extends AbstractMuleTestCase {

  @Test
  void cached() {
    final var adapter = from(mock(CachedConnectionProvider.class));

    assertThat(adapter,
               instanceOf(org.mule.runtime.api.connection.CachedConnectionProvider.class));
    assertThat(adapter,
               not(instanceOf(org.mule.runtime.core.internal.connection.adapter.XATransactionalConnectionProvider.class)));
  }

  @Test
  void cachedXa() {
    final var adapted = mock(CachedConnectionProvider.class,
                             withSettings().extraInterfaces(XATransactionalConnectionProvider.class));
    final var xaPoolingProfile = new PoolingProfile();
    when(((XATransactionalConnectionProvider) adapted).getXaPoolingProfile()).thenReturn(xaPoolingProfile);

    final var adapter = from(adapted);

    assertThat(adapter,
               instanceOf(org.mule.runtime.api.connection.CachedConnectionProvider.class));
    assertThat(adapter,
               instanceOf(org.mule.runtime.core.internal.connection.adapter.XATransactionalConnectionProvider.class));

    assertThat(((org.mule.runtime.core.internal.connection.adapter.XATransactionalConnectionProvider) adapter)
        .getXaPoolingProfile(), sameInstance(xaPoolingProfile));
  }

  @Test
  void pooled() {
    final var adapter = from(mock(PoolingConnectionProvider.class));

    assertThat(adapter,
               instanceOf(org.mule.runtime.api.connection.PoolingConnectionProvider.class));
    assertThat(adapter,
               not(instanceOf(org.mule.runtime.core.internal.connection.adapter.XATransactionalConnectionProvider.class)));
  }

  @Test
  void pooledXa() {
    final var adapted = mock(PoolingConnectionProvider.class,
                             withSettings().extraInterfaces(XATransactionalConnectionProvider.class));
    final var xaPoolingProfile = new PoolingProfile();
    when(((XATransactionalConnectionProvider) adapted).getXaPoolingProfile()).thenReturn(xaPoolingProfile);

    final var adapter = from(adapted);

    assertThat(adapter,
               instanceOf(org.mule.runtime.api.connection.PoolingConnectionProvider.class));
    assertThat(adapter,
               instanceOf(org.mule.runtime.core.internal.connection.adapter.XATransactionalConnectionProvider.class));

    assertThat(((org.mule.runtime.core.internal.connection.adapter.XATransactionalConnectionProvider) adapter)
        .getXaPoolingProfile(), sameInstance(xaPoolingProfile));
  }

  @Test
  void nullManagement() {
    final var adapter = from(mock(ConnectionProvider.class));

    assertThat(adapter,
               instanceOf(org.mule.runtime.api.connection.ConnectionProvider.class));
    assertThat(adapter,
               not(instanceOf(org.mule.runtime.core.internal.connection.adapter.XATransactionalConnectionProvider.class)));
  }

  @Test
  void nullManagementXa() {
    final var adapted = mock(ConnectionProvider.class,
                             withSettings().extraInterfaces(XATransactionalConnectionProvider.class));
    final var xaPoolingProfile = new PoolingProfile();
    when(((XATransactionalConnectionProvider) adapted).getXaPoolingProfile()).thenReturn(xaPoolingProfile);

    final var adapter = from(adapted);

    assertThat(adapter,
               instanceOf(org.mule.runtime.api.connection.ConnectionProvider.class));
    assertThat(adapter,
               instanceOf(org.mule.runtime.core.internal.connection.adapter.XATransactionalConnectionProvider.class));

    assertThat(((org.mule.runtime.core.internal.connection.adapter.XATransactionalConnectionProvider) adapter)
        .getXaPoolingProfile(), sameInstance(xaPoolingProfile));
  }

}
