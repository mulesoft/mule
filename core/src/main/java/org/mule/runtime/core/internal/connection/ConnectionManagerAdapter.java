/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;

/**
 * Interface for {@link ConnectionManager} implementations which expands its contract with non API functionality
 *
 * @since 4.0
 */
public interface ConnectionManagerAdapter extends ConnectionManager, Stoppable {

  /**
   * Returns the {@link RetryPolicyTemplate} that should be applied to the given {@code connectionProvider}
   *
   * @param connectionProvider a {@link ConnectionProvider}
   * @param <C> the generic type of the connection returned by the provider
   * @return a {@link RetryPolicyTemplate}
   */
  <C> RetryPolicyTemplate getRetryTemplateFor(ConnectionProvider<C> connectionProvider);

  /**
   * Returns the {@link ReconnectionConfig} that should be applied to the given {@code connectionProvider}
   *
   * @param connectionProvider a {@link ConnectionProvider}
   * @param <C> the generic type of the connection returned by the provider
   * @return a {@link ReconnectionConfig}
   */
  <C> ReconnectionConfig getReconnectionConfigFor(ConnectionProvider<C> connectionProvider);

  /**
   * When no {@link PoolingProfile} is specified by the user the {@link ConnectionManagerAdapter} will provide the default one to
   * configure the pool of connections
   *
   * @return a {@link PoolingProfile}
   */
  PoolingProfile getDefaultPoolingProfile();
}
