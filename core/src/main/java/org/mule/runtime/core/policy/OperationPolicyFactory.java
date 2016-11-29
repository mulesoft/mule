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
 * Factory for {@link OperationPolicy} instances.
 *
 * @since 4.0
 */
public interface OperationPolicyFactory {

  /**
   * Creates an {@link OperationPolicy}.
   *
   * @param policy the policy from which the {@link OperationPolicy} gets created.
   * @param operationPolicyParametersTransformer transformer from the operation parameters to a message and vice versa.
   * @param nextProcessor the next-operation processor implementation
   * @param operationParametersProcessor a processor that converts an event to the set of parameters to be sent by the
   *        operation based on the user configuration.

   * @return an {@link OperationPolicy} that performs the common logic related to policies.
   */
  OperationPolicy createOperationPolicy(Policy policy,
                                        Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer,
                                        Processor nextProcessor,
                                        OperationParametersProcessor operationParametersProcessor);

}
