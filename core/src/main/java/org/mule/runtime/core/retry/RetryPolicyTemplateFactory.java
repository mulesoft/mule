/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.retry;

import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.retry.async.AsynchronousRetryTemplate;
import org.mule.runtime.core.retry.policies.SimpleRetryPolicyTemplate;
import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;

public class RetryPolicyTemplateFactory extends AbstractAnnotatedObjectFactory<RetryPolicyTemplate> {

  private int frequency;
  private int count;
  private boolean blocking = true;

  @Override
  public RetryPolicyTemplate doGetObject() throws Exception {
    RetryPolicyTemplate retryPolicyTemplate = getRetryPolicyTemplate();
    if (blocking) {
      return retryPolicyTemplate;
    } else {
      return new AsynchronousRetryTemplate(retryPolicyTemplate);
    }
  }

  public void setFrequency(int frequency) {
    this.frequency = frequency;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public void setBlocking(boolean blocking) {
    this.blocking = blocking;
  }

  public int getFrequency() {
    return frequency;
  }

  public int getCount() {
    return count;
  }

  protected RetryPolicyTemplate getRetryPolicyTemplate() {
    return new SimpleRetryPolicyTemplate(frequency, count);
  }
}
