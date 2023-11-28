/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry;

import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.retry.DefaultReconnectionConfig;
import org.mule.runtime.extension.api.runtime.source.Source;

/**
 * Configures the default reconnection behaviour for a connected component.
 *
 * Whenever a connected {@link Processor} or {@link Source} doesn't specify a specific reconnection strategy, an instance of this
 * class should be used as a fallback.
 *
 * @since 4.6
 */
public interface ReconnectionConfig {

  /**
   * @param failsDeployment     whether the deployment should fail if the connectivity test that is performed on all connectors
   *                            doesn't pass after exhausting the associated reconnection strategy.
   * @param retryPolicyTemplate the reconnection strategy to use.
   * @return a {@link ReconnectionConfig} instance with the given configuration.
   */
  static ReconnectionConfig getReconnectionConfig(boolean failsDeployment, RetryPolicyTemplate retryPolicyTemplate) {
    return new DefaultReconnectionConfig(failsDeployment, retryPolicyTemplate);
  }

  /**
   * @return whether the deployment will fail in case the connectivity test that is performed on all connectors doesn't pass after
   *         exhausting this instance's reconnection strategy.
   */
  boolean isFailsDeployment();

  /**
   * @return the configured reconnection strategy.
   */
  RetryPolicyTemplate getRetryPolicyTemplate();

}
