/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.dsl.processor;

import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
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
    return new ReconnectionConfig(failsDeployment, retryPolicyTemplate);
  }

  public void setFailsDeployment(boolean failsDeployment) {
    this.failsDeployment = failsDeployment;
  }

  public void setRetryPolicyTemplate(RetryPolicyTemplate retryPolicyTemplate) {
    this.retryPolicyTemplate = retryPolicyTemplate;
  }
}
