/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.retry.policies.AbstractPolicyTemplate;

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
   * @param delegate the {@link ConnectionProvider} to be wrapped
   * @param poolingProfile a not {@code null} {@link PoolingProfile}
   * @param retryPolicyTemplate a {@link AbstractPolicyTemplate} which will hold the retry policy configured in the Mule
   *        Application
   */
  public PoolingConnectionProviderWrapper(ConnectionProvider<C> delegate, PoolingProfile poolingProfile,
                                          boolean disableValidation, RetryPolicyTemplate retryPolicyTemplate) {
    super(delegate, disableValidation, retryPolicyTemplate);
    this.poolingProfile = poolingProfile;
  }

  @Override
  public Optional<PoolingProfile> getPoolingProfile() {
    return Optional.ofNullable(poolingProfile);
  }
}
