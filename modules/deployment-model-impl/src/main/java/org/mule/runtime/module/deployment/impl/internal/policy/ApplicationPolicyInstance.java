/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyPointcut;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;

import java.util.Optional;

/**
 * Defines a policy provider for a given parametrized policy
 */
public interface ApplicationPolicyInstance extends Initialisable, Disposable {

  /**
   * @return the policy's pointcut used to determine whether to apply or ignore the policy when a request arrives. No null.
   */
  PolicyPointcut getPointcut();

  /**
   * @return order that must be used to apply the policy
   */
  int getOrder();

  /**
   * @return policy template from which the instance will be created
   */
  PolicyTemplate getPolicyTemplate();

  /**
   * @return policy to intercept the source execution
   */
  Optional<Policy> getSourcePolicy();

  /**
   * @return policy to intercept the operation execution
   */
  Optional<Policy> getOperationPolicy();
}
