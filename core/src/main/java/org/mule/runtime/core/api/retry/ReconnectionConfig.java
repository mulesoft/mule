/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry;

import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
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
   * @return a default {@link ReconnectionConfig} instance with default values.
   */
  static ReconnectionConfig defaultReconnectionConfig() {
    return new DefaultReconnectionConfig(false, new NoRetryPolicyTemplate());
  }

  /**
   * @param failsDeployment     whether the deployment should fail if the connectivity test that is performed on all connectors
   *                            doesn't pass after exhausting the associated reconnection strategy.
   * @param retryPolicyTemplate the reconnection strategy to use.
   * @return a default {@link ReconnectionConfig} instance with the given configuration.
   */
  static ReconnectionConfig defaultReconnectionConfig(boolean failsDeployment, RetryPolicyTemplate retryPolicyTemplate) {
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

  /**
   * Generates a {@link RetryPolicyTemplate} that behaves as expected regarding the deployment model defined by {@code this}
   * {@link ReconnectionConfig}, while maintaining the behaviour expected for the delegating policy.
   *
   * @param delegate the {@link RetryPolicyTemplate} with the policy and configuration that should be finally applied
   * @return a {@link RetryPolicyTemplate} that is configured with the current deployment configuration, while using the
   *         delegate's RetryPolicy.
   */
  RetryPolicyTemplate getRetryPolicyTemplate(RetryPolicyTemplate delegate);

}
