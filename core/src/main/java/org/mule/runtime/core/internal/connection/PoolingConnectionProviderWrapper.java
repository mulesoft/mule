/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.retry.policies.AbstractPolicyTemplate;

import java.util.Optional;

/**
 * A {@link ConnectionProviderWrapper} which decorates the {@link #delegate} with a user configured {@link PoolingProfile} or the
 * default one if is was not supplied by the user.
 *
 * @since 4.0
 */
public final class PoolingConnectionProviderWrapper<Connection> extends ConnectionProviderWrapper<Connection> {

  private final PoolingProfile poolingProfile;
  private final boolean disableValidation;
  private final RetryPolicyTemplate retryPolicyTemplate;

  /**
   * Creates a new instance
   *
   * @param delegate the {@link ConnectionProvider} to be wrapped
   * @param poolingProfile a not {@code null} {@link PoolingProfile}
   * @param retryPolicyTemplate a {@link AbstractPolicyTemplate} which will hold the retry policy configured in the Mule
   *        Application
   */
  public PoolingConnectionProviderWrapper(ConnectionProvider<Connection> delegate, PoolingProfile poolingProfile,
                                          boolean disableValidation, RetryPolicyTemplate retryPolicyTemplate) {
    super(delegate);
    this.poolingProfile = poolingProfile;
    this.disableValidation = disableValidation;
    this.retryPolicyTemplate = retryPolicyTemplate;
  }

  /**
   * Delegates the responsibility of validating the connection to the delegated {@link ConnectionProvider} If
   * {@link #disableValidation} if {@code true} then the validation is skipped, returning
   * {@link ConnectionValidationResult#success()}
   *
   * @param connection a given connection
   * @return A {@link ConnectionValidationResult} returned by the delegated {@link ConnectionProvider}
   */
  @Override
  public ConnectionValidationResult validate(Connection connection) {
    if (disableValidation) {
      return ConnectionValidationResult.success();
    }
    return getDelegate().validate(connection);
  }

  /**
   * @return a {@link RetryPolicyTemplate} with the configured values in the Mule Application.
   */
  @Override
  public RetryPolicyTemplate getRetryPolicyTemplate() {
    return retryPolicyTemplate;
  }

  @Override
  public Optional<PoolingProfile> getPoolingProfile() {
    return Optional.ofNullable(poolingProfile);
  }
}
