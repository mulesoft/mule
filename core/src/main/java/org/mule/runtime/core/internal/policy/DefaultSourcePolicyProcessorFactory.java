/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyStateHandler;
import org.mule.runtime.core.api.processor.Processor;

/**
 * Default implementation for {@link SourcePolicyProcessorFactory}.
 *
 * @since 4.0
 */
public class DefaultSourcePolicyProcessorFactory implements SourcePolicyProcessorFactory {

  private final PolicyStateHandler policyStateHandler;

  /**
   * Creates a new instance of the default factory for {@link SourcePolicy}s.
   *
   * @param policyStateHandler the state handler for policies.
   */
  public DefaultSourcePolicyProcessorFactory(PolicyStateHandler policyStateHandler) {
    this.policyStateHandler = policyStateHandler;
  }

  @Override
  public Processor createSourcePolicy(Policy policy, Processor nextProcessor) {
    return new SourcePolicyProcessor(policy, policyStateHandler, nextProcessor);
  }
}
