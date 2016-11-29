/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;

import java.util.Optional;

/**
 * Default implementation for {@link SourcePolicyFactory}.
 *
 * @since 4.0
 */
public class DefaultSourcePolicyFactory implements SourcePolicyFactory {

  private final PolicyStateHandler policyStateHandler;

  /**
   * Creates a new instance of the default factory for {@link SourcePolicy}s.
   *
   * @param policyStateHandler the state handler for policies.
   */
  public DefaultSourcePolicyFactory(PolicyStateHandler policyStateHandler) {
    this.policyStateHandler = policyStateHandler;
  }

  @Override
  public SourcePolicy createSourcePolicy(Policy policy,
                                         Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer,
                                         Processor nextProcessor,
                                         MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {
    return new DefaultSourcePolicy(policy, sourcePolicyParametersTransformer, policyStateHandler, nextProcessor,
                                   messageSourceResponseParametersProcessor);
  }
}
