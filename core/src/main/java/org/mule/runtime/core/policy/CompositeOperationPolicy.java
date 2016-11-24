/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;
import java.util.Optional;

/**
 * {@link OperationPolicy} created from a list of {@link Policy}.
 * <p>
 * Implements the template methods from {@link AbstractCompositePolicy} required to work with operation policies.
 *
 * @since 4.0
 */
public class CompositeOperationPolicy extends
    AbstractCompositePolicy<OperationPolicyParametersTransformer, OperationParametersProcessor> implements OperationPolicy {


  private Event nextOperationResponse;
  private OperationPolicyFactory operationPolicyFactory;

  /**
   * Creates a new composite policy.
   *
   * @param parameterizedPolicies list of {@link Policy} to chain together.
   * @param operationPolicyParametersTransformer transformer from the operation parameters to a message and vice versa.
   * @param operationPolicyFactory factory for creating each {@link OperationPolicy} from a {@link Policy}
   */
  public CompositeOperationPolicy(List<Policy> parameterizedPolicies,
                                  Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer,
                                  OperationPolicyFactory operationPolicyFactory) {
    super(parameterizedPolicies, operationPolicyParametersTransformer);
    this.operationPolicyFactory = operationPolicyFactory;
  }

  /**
   * Stores the operation result so all the chains after the operation execution are executed with the actual operation result and
   * not a modified version from another policy.
   */
  @Override
  protected Event processNextOperation(Processor nextOperation, Event event) throws MuleException {
    nextOperationResponse = nextOperation.process(event);
    return nextOperationResponse;
  }

  /**
   * Always uses the stored result of {@code processNextOperation} so all the chains after the operation execution are executed
   * with the actual operation result and not a modified version from another policy.
   */
  @Override
  protected Event processPolicy(Policy policy, Processor nextProcessor,
                                Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer,
                                OperationParametersProcessor operationParametersProcessor,
                                Event event)
      throws Exception {
    OperationPolicy defaultOperationPolicy =
        operationPolicyFactory.createOperationPolicy(policy, operationPolicyParametersTransformer);
    defaultOperationPolicy.process(event, nextProcessor, operationParametersProcessor);
    return nextOperationResponse;
  }

}
