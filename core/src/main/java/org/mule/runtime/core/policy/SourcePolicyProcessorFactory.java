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
 * Factory for {@link Processor} instances created from a {@link Policy}
 *
 * @since 4.0
 */
public interface SourcePolicyProcessorFactory {

  /**
   * Creates an {@link SourcePolicy}.
   *
   * @param policy the policy from which the {@link SourcePolicy} gets created.
   * @param sourcePolicyParametersTransformer transformer from the source response parameters to a message and vice versa.
   * @param nextProcessor the next processor in the chain.
   * @param messageSourceResponseParametersProcessor processor that generate the response and error response parameters.
   * @return an {@link SourcePolicy} that performs the common logic related to policies.
   */
  Processor createSourcePolicy(Policy policy,
                               Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer,
                               Processor nextProcessor,
                               MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor);

}
