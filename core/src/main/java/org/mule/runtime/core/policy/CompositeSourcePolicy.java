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
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;
import java.util.Optional;

/**
 * {@link SourcePolicy} created from a list of {@link Policy}.
 * <p>
 * Takes care of chaining the list of {@link Policy} to create a single policy that can be applied to a source.
 *
 * @since 4.0
 */
public class CompositeSourcePolicy implements SourcePolicy {

  private List<Policy> parameterizedPolicies;
  private Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer;
  private PolicyStateHandler policyStateHandler;

  /**
   * Creates a new composite policy.
   *
   * @param parameterizedPolicies list of {@link Policy} to chain together.
   * @param sourcePolicyParametersTransformer transformer from the response parameters to a message and vice versa.
   * @param policyStateHandler state handler for policies execution.
   */
  public CompositeSourcePolicy(List<Policy> parameterizedPolicies,
                               Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer,
                               PolicyStateHandler policyStateHandler) {
    this.parameterizedPolicies = parameterizedPolicies;
    this.sourcePolicyParametersTransformer = sourcePolicyParametersTransformer;
    this.policyStateHandler = policyStateHandler;
  }

  /**
   * When this policy is processed, it will use the {@link CompositeSourcePolicy.NextOperationCall} which will keep track of the
   * different policies to be applied and the current index of the policy under execution.
   * <p>
   * The first time, the first policy in the {@code parameterizedPolicies} will be executed, it will receive as it next operation
   * the same instance of {@link CompositeSourcePolicy.NextOperationCall}, and since
   * {@link CompositeSourcePolicy.NextOperationCall} keeps track of the policy executed, it will execute the following policy in
   * the chain until the finally policy it's executed in which case then next operation of it, it will be the flow execution.
   */
  @Override
  public Event process(Event sourceEvent, Processor nextOperation,
                       MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor)
      throws Exception {
    return new NextOperationCall(sourceEvent, nextOperation, messageSourceResponseParametersProcessor).process(sourceEvent);
  }

  /**
   * Inner class that implements the actually chaining of policies.
   */
  public class NextOperationCall implements Processor {

    private int index = 0;
    private Processor nonPolicyNextOperation;
    private Event sourceEvent;
    private MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor;

    public NextOperationCall(Event sourceEvent, Processor nonPolicyNextOperation,
                             MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {
      this.nonPolicyNextOperation = nonPolicyNextOperation;
      this.sourceEvent = sourceEvent;
      this.messageSourceResponseParametersProcessor = messageSourceResponseParametersProcessor;
    }

    @Override
    public Event process(Event event) throws MuleException {
      if (index >= parameterizedPolicies.size()) {
        return nonPolicyNextOperation.process(event);
      }
      Policy policy = parameterizedPolicies.get(index);
      index++;
      DefaultSourcePolicy defaultSourcePolicy =
          new DefaultSourcePolicy(policy.getPolicyChain(), sourcePolicyParametersTransformer, policyStateHandler);
      try {
        return defaultSourcePolicy.process(sourceEvent, this, messageSourceResponseParametersProcessor);
      } catch (MuleException e) {
        throw e;
      } catch (Exception e) {
        throw new DefaultMuleException(e);
      }
    }
  }
}
