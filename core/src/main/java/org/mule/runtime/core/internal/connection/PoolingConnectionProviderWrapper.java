/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static java.util.Optional.ofNullable;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;

import java.util.Optional;

/**
 * A {@link ConnectionProviderWrapper} which decorates the {@link #delegate} with a user configured {@link PoolingProfile} or the
 * default one if is was not supplied by the user.
 *
 * @since 4.0
 */
public final class PoolingConnectionProviderWrapper<C> extends ReconnectableConnectionProviderWrapper<C> {

  private final PoolingProfile poolingProfile;

  /**
   * Creates a new instance
   *
   * @param delegate           the {@link ConnectionProvider} to be wrapped
   * @param poolingProfile     a not {@code null} {@link PoolingProfile}
   * @param reconnectionConfig a {@link ReconnectionConfig}
   */
  public PoolingConnectionProviderWrapper(ConnectionProvider<C> delegate,
                                          PoolingProfile poolingProfile,
                                          ReconnectionConfig reconnectionConfig) {
    super(delegate, reconnectionConfig);
    this.poolingProfile = poolingProfile;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<PoolingProfile> getPoolingProfile() {
    return ofNullable(poolingProfile);
  }
}
