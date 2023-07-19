/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.policy;

import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;

/**
 * Creates {@link ApplicationPolicyInstance} instances
 */
public interface PolicyInstanceProviderFactory {

  /**
   * Creates a new policy
   *
   * @param application     application when the policy is applied. Non null
   * @param policyTemplate  template of the policy being applied. Non null.
   * @param parametrization parameters used to configure the template. Non null/
   * @return
   */
  ApplicationPolicyInstance create(Application application, PolicyTemplate policyTemplate, PolicyParametrization parametrization);
}
