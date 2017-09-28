/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.processor;

import static org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate.DEFAULT_FREQUENCY;
import static org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate.DEFAULT_RETRY_COUNT;

import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.retry.RetryNotifier;
import org.mule.runtime.core.api.retry.async.AsynchronousRetryTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;
import org.mule.runtime.dsl.api.component.ObjectFactory;

import javax.inject.Inject;

/**
 * {@link ObjectFactory} for reconnection configuration.
 *
 * It will return a {@link RetryPolicyTemplate} that may run synchronously or asynchronously based on the {@code blocking}
 * configuration.
 *
 * @since 4.0
 */
public class RetryPolicyTemplateObjectFactory extends AbstractComponentFactory<RetryPolicyTemplate> {

  private boolean blocking;
  private Integer count = DEFAULT_RETRY_COUNT;
  private Integer frequency = DEFAULT_FREQUENCY;
  private RetryNotifier retryNotifier;

  @Inject
  private NotificationDispatcher notificationFirer;

  /**
   * @param blocking true if the policy must run synchronously when invoked, false if it must run asynchronously.
   */
  public void setBlocking(boolean blocking) {
    this.blocking = blocking;
  }

  /**
   * @param count the number of retries to execute.
   */
  public void setCount(Integer count) {
    this.count = count;
  }

  /**
   * @param frequency time between retries.
   */
  public void setFrequency(Integer frequency) {
    this.frequency = frequency;
  }

  /**
   * @param retryNotifier the retry notifier to use when retrying in the template
   */
  public void setRetryNotifier(RetryNotifier retryNotifier) {
    this.retryNotifier = retryNotifier;
  }

  @Override
  public RetryPolicyTemplate doGetObject() throws Exception {
    // MULE-13092 ExecutionMediator should use scheduler for retry policy
    SimpleRetryPolicyTemplate retryPolicyTemplate = new SimpleRetryPolicyTemplate(frequency, count);
    retryPolicyTemplate.setNotificationFirer(notificationFirer);
    if (retryNotifier != null) {
      retryPolicyTemplate.setNotifier(retryNotifier);
    }
    if (!blocking) {
      return new AsynchronousRetryTemplate(retryPolicyTemplate);
    }
    return retryPolicyTemplate;
  }
}
