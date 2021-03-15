/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.retry;

import static java.lang.Boolean.valueOf;
import static java.lang.System.getProperty;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.async.AsynchronousRetryTemplate;
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

  public static final String DISABLE_ASYNC_RETRY_POLICY_ON_SOURCES = SYSTEM_PROPERTY_PREFIX + "disableAsyncRetryPolicyOnSources";

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
    this.retryPolicyTemplate = getRetryPolicyTemplate(retryPolicyTemplate);
  }

  public boolean isFailsDeployment() {
    return failsDeployment;
  }

  public RetryPolicyTemplate getRetryPolicyTemplate() {
    return retryPolicyTemplate;
  }

  /**
   * Generates a {@link RetryPolicyTemplate} that behaves as expected regarding the deployment model defined by {@code this}
   * {@link ReconnectionConfig}, while maintaining the behaviour expected for the delegating policy.
   *
   * @param delegate the {@link RetryPolicyTemplate} with the policy and configuration that should be finally applied
   * @return a {@link RetryPolicyTemplate} that is configured with the current deployment configuration, while using the
   *         delegate's RetryPolicy.
   */
  public RetryPolicyTemplate getRetryPolicyTemplate(RetryPolicyTemplate delegate) {
    if (delegate == null) {
      return this.retryPolicyTemplate;
    }

    if (failsDeployment) {
      return getBlockingTemplate(delegate);
    }

    // Just for testing mode, if disable async use blocking reconnection
    if (isDisableAsyncReconnection()) {
      return getBlockingTemplate(delegate);
    }

    return getAsyncTemplate(delegate);
  }

  private boolean isDisableAsyncReconnection() {
    return valueOf(getProperty(DISABLE_ASYNC_RETRY_POLICY_ON_SOURCES, "false"));
  }

  /**
   * @return an {@link AsynchronousRetryTemplate} configured with the given {@code delegate}. This {@link RetryPolicyTemplate}
   *         will not block the execution while performing the retries.
   */
  private RetryPolicyTemplate getAsyncTemplate(RetryPolicyTemplate delegate) {
    if (delegate instanceof AsynchronousRetryTemplate) {
      return delegate;
    }
    return new AsynchronousRetryTemplate(delegate);
  }

  /**
   * @return a {@link RetryPolicyTemplate} configured with the given {@code delegate} policy, but blocking the execution while
   *         performing the retries.
   */
  private RetryPolicyTemplate getBlockingTemplate(RetryPolicyTemplate delegate) {
    if (delegate == null) {
      return this.retryPolicyTemplate;
    }
    if (delegate instanceof AsynchronousRetryTemplate) {
      return getBlockingTemplate(((AsynchronousRetryTemplate) delegate).getDelegate());
    }
    return delegate;
  }
}
