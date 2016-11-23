/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;
import java.util.Optional;

/**
 * {@link OperationPolicy} created from a list of {@link Policy}.
 * <p>
 * Takes care of chaining the list of {@link Policy} to create a single
 * policy that can be applied to a source.
 *
 * @since 4.0
 */
public class CompositeOperationPolicy implements OperationPolicy {

  private List<Policy> parameterizedPolicies;
  private Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer;
  private PolicyStateHandler policyStateHandler;

  /**
   * Creates a new composite policy.
   *
   * @param parameterizedPolicies                list of {@link Policy} to chain together.
   * @param operationPolicyParametersTransformer transformer from the operation parameters to a message and vice versa.
   * @param policyStateHandler                   state handler for policies execution.
   */
  public CompositeOperationPolicy(List<Policy> parameterizedPolicies,
                                  Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer,
                                  PolicyStateHandler policyStateHandler) {
    this.parameterizedPolicies = parameterizedPolicies;
    this.operationPolicyParametersTransformer = operationPolicyParametersTransformer;
    this.policyStateHandler = policyStateHandler;
  }

  /**
   * When this policy is processed, it will use the {@link NextOperationCall} which will keep track of the different policies to be applied and
   * the current index of the policy under execution.
   * <p>
   * The first time, the first policy in the {@code parameterizedPolicies} will be executed, it will receive as it next operation the same
   * instance of {@link NextOperationCall}, and since {@link NextOperationCall} keeps track of the policy executed, it will execute the following
   * policy in the chain until the finally policy it's executed in which case then next operation of it, it will be the operation execution.
   */
  @Override
  public Event process(Event operationEvent, Processor nextProcessor, OperationParametersProcessor operationParametersProcessor)
      throws Exception {
    return new NextOperationCall(operationEvent, nextProcessor, operationParametersProcessor).process(operationEvent);
  }

  /**
   * Inner class that implements the actually chaining of policies.
   */
  public class NextOperationCall implements Processor {

    private int index = 0;
    private Processor operationProcessor;
    private Event operationEvent;
    private OperationParametersProcessor operationParametersProcessor;
    private Event operationResponseEvent;

    public NextOperationCall(Event operationEvent, Processor operationProcessor,
                             OperationParametersProcessor operationParametersProcessor) {
      this.operationEvent = operationEvent;
      this.operationProcessor = operationProcessor;
      this.operationParametersProcessor = operationParametersProcessor;
    }

    @Override
    public Event process(Event event) throws MuleException {
      if (index >= parameterizedPolicies.size()) {
        operationResponseEvent = operationProcessor.process(event);
        return operationResponseEvent;
      }
      Policy policy = parameterizedPolicies.get(index);
      index++;
      DefaultOperationPolicy defaultOperationPolicy =
          new DefaultOperationPolicy(policy.getPolicyChain(), operationPolicyParametersTransformer,
                                     policyStateHandler);
      try {
        defaultOperationPolicy.process(operationEvent, this, operationParametersProcessor);
        return operationResponseEvent;
      } catch (MuleException e) {
        throw e;
      } catch (Exception e) {
        throw new DefaultMuleException(e);
      }
    }
  }
}
