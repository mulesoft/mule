/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.retry.async.AsynchronousRetryTemplate;
import org.mule.runtime.core.retry.policies.SimpleRetryPolicyTemplate;

/**
 * {@link org.mule.runtime.config.spring.dsl.api.ObjectFactory} for reconnection configuration.
 *
 * It will return a {@link org.mule.runtime.core.api.retry.RetryPolicyTemplate} that may run synchronously or asynchronously based
 * on the {@code blocking} configuration.
 *
 * @since 4.0
 */
public class RetryPolicyTemplateObjectFactory implements ObjectFactory<RetryPolicyTemplate>, MuleContextAware {

  private boolean blocking;
  private Integer count = SimpleRetryPolicyTemplate.DEFAULT_RETRY_COUNT;
  private Integer frequency = SimpleRetryPolicyTemplate.DEFAULT_FREQUENCY;
  private MuleContext muleContext;

  @Override
  public RetryPolicyTemplate getObject() throws Exception {
    SimpleRetryPolicyTemplate retryPolicyTemplate = new SimpleRetryPolicyTemplate(frequency, count);
    retryPolicyTemplate.setMuleContext(muleContext);
    if (!blocking) {
      return new AsynchronousRetryTemplate(retryPolicyTemplate);
    }
    return retryPolicyTemplate;
  }

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

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
