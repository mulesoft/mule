/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;

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

  private final boolean disableValidation;
  private final RetryPolicyTemplate retryPolicyTemplate;

  /**
   * Creates a new instance
   *
   * @param delegate the {@link ConnectionProvider} to be wrapped
   * @param disableValidation whether to skip connection validation upon invocations of {@link #validate(Object)}
   * @param retryPolicyTemplate The {@link RetryPolicyTemplate} for retrying failed connection attempts
   */
  public ReconnectableConnectionProviderWrapper(ConnectionProvider<C> delegate,
                                                boolean disableValidation,
                                                RetryPolicyTemplate retryPolicyTemplate) {
    super(delegate);
    this.disableValidation = disableValidation;
    this.retryPolicyTemplate = retryPolicyTemplate;
  }

  /**
   * Delegates the responsibility of validating the connection to the delegated {@link ConnectionProvider}.
   * If {@link #disableValidation} is {@code true}, then the validation is skipped, returning
   * {@link ConnectionValidationResult#success()}
   *
   * @param connection a given connection
   * @return A {@link ConnectionValidationResult} returned by the delegated {@link ConnectionProvider}
   */
  @Override
  public ConnectionValidationResult validate(C connection) {
    if (disableValidation) {
      return ConnectionValidationResult.success();
    }
    return getDelegate().validate(connection);
  }

  /**
   * @return a {@link RetryPolicyTemplate} with the configured values in the Mule Application.
   */
  public RetryPolicyTemplate getRetryPolicyTemplate() {
    return retryPolicyTemplate;
  }

}
