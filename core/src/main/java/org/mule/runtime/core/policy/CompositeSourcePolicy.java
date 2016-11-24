/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;
import java.util.Optional;

/**
 * {@link SourcePolicy} created from a list of {@link Policy}.
 * <p>
 * Implements the template methods from {@link AbstractCompositePolicy} required to work with source policies.
 *
 * @since 4.0
 */
public class CompositeSourcePolicy extends
    AbstractCompositePolicy<SourcePolicyParametersTransformer, MessageSourceResponseParametersProcessor> implements SourcePolicy {

  private SourcePolicyFactory sourcePolicyFactory;

  public CompositeSourcePolicy(List<Policy> parameterizedPolicies,
                               Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer,
                               SourcePolicyFactory sourcePolicyFactory) {
    super(parameterizedPolicies, sourcePolicyParametersTransformer);
    this.sourcePolicyFactory = sourcePolicyFactory;
  }

  /**
   * Executes the flow and returns it's value since it's going to be used by the policy wrapping the flow.
   */
  @Override
  protected Event processNextOperation(Processor nextOperation, Event event) throws MuleException {
    return nextOperation.process(event);
  }

  /**
   * Always return the policy execution / flow execution result so the next policy executes with the modified version of the
   * wrapped policy / flow.
   */
  @Override
  protected Event processPolicy(Policy policy, Processor nextProcessor,
                                Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer,
                                MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor,
                                Event event)
      throws Exception {
    SourcePolicy defaultSourcePolicy = sourcePolicyFactory.createSourcePolicy(policy, sourcePolicyParametersTransformer);
    return defaultSourcePolicy.process(event, nextProcessor, messageSourceResponseParametersProcessor);
  }

}
