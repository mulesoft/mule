/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.retry;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.extension.api.runtime.source.Source;

/**
 * Configures the default reconnection behaviour for a connected component.
 *
 * Whenever a connected {@link Processor} or {@link Source} doesn't specify a specific reconnection strategy, an instance of this
 * class should be used as a fallback.
 *
 * @since 4.0
 */
public class ReconnectionConfig extends AbstractComponent {

  /**
   * When the application is deployed, a connectivity test is performed on all connectors. If set to {@code true}, deployment will
   * fail if the test doesn't pass after exhausting the associated reconnection strategy
   */
  private final boolean failsDeployment;

  /**
   * The reconnection strategy to use.
   */
  private final RetryPolicyTemplate retryPolicyTemplate;

  /**
   * @return a new instance with default values
   */
  public static ReconnectionConfig getDefault() {
    return new ReconnectionConfig(false, new NoRetryPolicyTemplate());
  }

  public ReconnectionConfig(boolean failsDeployment, RetryPolicyTemplate retryPolicyTemplate) {
    this.failsDeployment = failsDeployment;
    this.retryPolicyTemplate = retryPolicyTemplate;
  }

  public boolean isFailsDeployment() {
    return failsDeployment;
  }

  public RetryPolicyTemplate getRetryPolicyTemplate() {
    return retryPolicyTemplate;
  }
}
