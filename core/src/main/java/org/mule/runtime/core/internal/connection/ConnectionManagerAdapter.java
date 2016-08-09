/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.retry.RetryPolicy;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;

/**
 * Interface for {@link ConnectionManager} implementations which expands its contract with non API functionality
 *
 * @since 4.0
 */
public interface ConnectionManagerAdapter extends ConnectionManager, Stoppable {

  /**
   * When no {@link RetryPolicyTemplate} is specified by the user the {@link ConnectionManagerAdapter} will provide the default
   * one to create the required {@link RetryPolicy}s instances.
   *
   * @return a {@link RetryPolicyTemplate}
   */
  RetryPolicyTemplate getDefaultRetryPolicyTemplate();

  /**
   * When no {@link PoolingProfile} is specified by the user the {@link ConnectionManagerAdapter} will provide the default one to
   * configure the pool of connections
   *
   * @return a {@link PoolingProfile}
   */
  PoolingProfile getDefaultPoolingProfile();
}
