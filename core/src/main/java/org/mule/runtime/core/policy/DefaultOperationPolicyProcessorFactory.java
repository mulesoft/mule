/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;

import java.util.Optional;

/**
 * Default implementation for {@link OperationPolicyProcessorFactory}.
 *
 * @since 4.0
 */
public class DefaultOperationPolicyProcessorFactory implements OperationPolicyProcessorFactory
{

  private final PolicyStateHandler policyStateHandler;

  public DefaultOperationPolicyProcessorFactory(PolicyStateHandler policyStateHandler) {
    this.policyStateHandler = policyStateHandler;
  }

  @Override
  public Processor createOperationPolicy(Policy policy,
                                               Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer,
                                               Processor nextProcessor,
                                               OperationParametersProcessor operationParametersProcessor) {
    return new OperationPolicyProcessor(policy, operationPolicyParametersTransformer, policyStateHandler, nextProcessor,
                                        operationParametersProcessor);
  }
}
