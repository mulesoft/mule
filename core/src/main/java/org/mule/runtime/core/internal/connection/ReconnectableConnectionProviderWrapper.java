/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static java.util.Optional.ofNullable;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;

import java.util.Optional;

/**
 * A {@link ConnectionProviderWrapper} which includes a {@link RetryPolicyTemplate}
 * for reconnecting in case of problems establishing the connection.
 *
 * It also contains the ability to skip connection validation.
 *
 * @param <C> The generic type of the connections provided by the {@link #delegate}
 *           @since 4.0
 */
public class ReconnectableConnectionProviderWrapper<C> extends ConnectionProviderWrapper<C> {

  private final ReconnectionConfig reconnectionConfig;

  /**
   * Creates a new instance
   *
   * @param delegate the {@link ConnectionProvider} to be wrapped
   * @param reconnectionConfig The {@link ReconnectionConfig} for retrying failed connection attempts
   */
  public ReconnectableConnectionProviderWrapper(ConnectionProvider<C> delegate, ReconnectionConfig reconnectionConfig) {
    super(delegate);
    this.reconnectionConfig = reconnectionConfig;
  }

  /**
   * Delegates the responsibility of validating the connection to the delegated {@link ConnectionProvider}.
   *
   * @param connection a given connection
   * @return A {@link ConnectionValidationResult} returned by the delegated {@link ConnectionProvider}
   */
  @Override
  public ConnectionValidationResult validate(C connection) {
    return getDelegate().validate(connection);
  }

  @Override
  public Optional<ReconnectionConfig> getReconnectionConfig() {
    return ofNullable(reconnectionConfig);
  }
}
