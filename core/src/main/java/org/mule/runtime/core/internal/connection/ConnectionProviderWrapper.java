/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.connection;

import static java.util.Optional.empty;

import org.mule.runtime.api.config.HasPoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.retry.HasReconnectionConfig;

import java.util.Optional;

/**
 * Base contract for wrappers of {@link ConnectionProvider} instances
 *
 * @param <C> the generic type of the connections that {@link #getDelegate()} produces
 * @since 4.3.0
 */
public interface ConnectionProviderWrapper<C>
    extends ConnectionProvider<C>, HasPoolingProfile, HasReconnectionConfig, HasDelegate<C>, Lifecycle {

  RetryPolicyTemplate getRetryPolicyTemplate();

  /**
   * @return the {@link ConnectionManagementType} of the delegate {@link ConnectionProvider}.
   */
  ConnectionManagementType getConnectionManagementType();

  /**
   * @return The name of the config that owns this {@link ConnectionProvider}.
   *
   * @since 4.5
   */
  default Optional<String> getOwnerConfigName() {
    ConnectionProvider<C> delegate = getDelegate();
    if (delegate instanceof ConnectionProviderWrapper) {
      return ((ConnectionProviderWrapper<C>) delegate).getOwnerConfigName();
    }
    return empty();
  }
}
