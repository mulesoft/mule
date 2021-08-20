/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.connection;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.internal.retry.HasReconnectionConfig;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;

import java.util.Optional;

/**
 * A {@link ConnectionProviderWrapper} which can resolve the name of the config that owns this {@link ConnectionProvider}
 *
 * @since 4.5
 */
public class ConfigNameResolverConnectionProviderWrapper<C> extends AbstractConnectionProviderWrapper<C> {

  private final String ownerConfigName;

  /**
   * Creates a new instance
   *
   * @param delegate        The {@link ConnectionProvider} to be wrapped
   * @param ownerConfigName The name of the owning configuration
   */
  public ConfigNameResolverConnectionProviderWrapper(ConnectionProvider<C> delegate, String ownerConfigName) {
    super(delegate);
    this.ownerConfigName = ownerConfigName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ReconnectionConfig> getReconnectionConfig() {
    ConnectionProvider<C> delegate = getDelegate();
    if (delegate instanceof HasReconnectionConfig) {
      return ((HasReconnectionConfig) delegate).getReconnectionConfig();
    }
    return empty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<String> getOwnerConfigName() {
    return of(ownerConfigName);
  }
}
