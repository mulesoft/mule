/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import org.mule.runtime.core.policy.PolicyPointcut;
import org.mule.runtime.core.policy.PolicyProvider;

/**
 * Defines a policy provider for a given parametrized policy
 */
public interface PolicyInstanceProvider extends PolicyProvider {

  /**
   * @return the policy's pointcut used to determine whether to apply or ignore the policy when a request arrives. No null.
   */
  PolicyPointcut getPointcut();

  /**
   * Disposes the instance releasing any held resources
   */
  void dispose();
}
