/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.processor;

import static org.mule.runtime.core.api.retry.ReconnectionConfig.defaultReconnectionConfig;

import org.mule.runtime.core.api.retry.ReconnectionConfig;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;
import org.mule.runtime.dsl.api.component.ComponentFactory;

/**
 * {@link ComponentFactory} which yields instances of {@link ReconnectionConfig}
 *
 * @since 4.0
 */
public class ReconnectionConfigObjectFactory extends AbstractComponentFactory<ReconnectionConfig> {

  private boolean failsDeployment = false;
  private RetryPolicyTemplate retryPolicyTemplate = new NoRetryPolicyTemplate();

  @Override
  public ReconnectionConfig doGetObject() throws Exception {
    return defaultReconnectionConfig(failsDeployment, retryPolicyTemplate);
  }

  public void setFailsDeployment(boolean failsDeployment) {
    this.failsDeployment = failsDeployment;
  }

  public void setRetryPolicyTemplate(RetryPolicyTemplate retryPolicyTemplate) {
    this.retryPolicyTemplate = retryPolicyTemplate;
  }
}
