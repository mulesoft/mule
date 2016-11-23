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
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;
import java.util.Optional;

/**
 * {@link OperationPolicy} created from a list of {@link ParameterizedPolicy}.
 * <p>
 * Takes care of chaining the list of {@link ParameterizedPolicy} to create a single
 * policy that can be applied to a source.
 *
 * @since 4.0
 */
public class CompositeOperationPolicy implements OperationPolicy {

  private List<ParameterizedPolicy> parameterizedPolicies;
  private Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer;
  private PolicyStateHandler policyStateHandler;

  /**
   * Creates a new composite policy.
   *
   * @param parameterizedPolicies list of {@link ParameterizedPolicy} to chain together.
   * @param operationPolicyParametersTransformer transformer from the operation parameters to a message and vice versa.
   * @param policyStateHandler state handler for policies execution.
   */
  public CompositeOperationPolicy(List<ParameterizedPolicy> parameterizedPolicies,
                                  Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer,
                                  PolicyStateHandler policyStateHandler) {
    this.parameterizedPolicies = parameterizedPolicies;
    this.operationPolicyParametersTransformer = operationPolicyParametersTransformer;
    this.policyStateHandler = policyStateHandler;
  }

  /**
   * {@inheritDoc}
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
      ParameterizedPolicy parameterizedPolicy = parameterizedPolicies.get(index);
      index++;
      DefaultOperationPolicy defaultOperationPolicy =
          new DefaultOperationPolicy(parameterizedPolicy.getPolicyChain(), operationPolicyParametersTransformer,
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
