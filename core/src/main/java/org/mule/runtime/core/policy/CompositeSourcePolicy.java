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
import org.mule.runtime.core.execution.ModuleFlowProcessingPhaseTemplate;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * {@link SourcePolicy} created from a list of {@link Policy}.
 * <p>
 * Implements the template methods from {@link AbstractCompositePolicy} required to work with source policies.
 *
 * @since 4.0
 */
public class CompositeSourcePolicy extends
    AbstractCompositePolicy<SourcePolicyParametersTransformer, MessageSourceResponseParametersProcessor> implements SourcePolicy {

  private final Processor flowExecutionProcessor;
  private SourcePolicyFactory sourcePolicyFactory;
  private Event flowExecutionResponse;

  public CompositeSourcePolicy(List<Policy> parameterizedPolicies,
                               Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer,
                               SourcePolicyFactory sourcePolicyFactory, Processor flowExecutionProcessor,
                               MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {
    super(parameterizedPolicies, sourcePolicyParametersTransformer, messageSourceResponseParametersProcessor);
    this.sourcePolicyFactory = sourcePolicyFactory;
    this.flowExecutionProcessor = flowExecutionProcessor;
  }

  /**
   * Executes the flow and returns it's value since it's going to be used by the policy wrapping the flow.
   */
  @Override
  protected Event processNextOperation(Event event) throws MuleException {
    flowExecutionResponse = flowExecutionProcessor.process(event);
    return flowExecutionResponse;
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
    SourcePolicy defaultSourcePolicy =
        sourcePolicyFactory.createSourcePolicy(policy, sourcePolicyParametersTransformer, nextProcessor, messageSourceResponseParametersProcessor);
    return defaultSourcePolicy.process(event).getExecutionResult();
  }

  @Override
  public SourcePolicyResult process(Event sourceEvent) throws Exception {
    Event event = processPolicies(sourceEvent);
    return new SourcePolicyResult() {

      @Override
      public Event getExecutionResult() {
        return event;
      }

      @Override
      public Map<String, Object> getResponseParameters() {
        return getParametersTransformer()
            .map(parametersTransformer -> parametersTransformer.fromMessageToSuccessResponseParameters(event.getMessage()))
            .orElseGet(() -> getParametersProcessor().getSuccessfulExecutionResponseParametersFunction().apply(event));
      }

      @Override
      public Map<String, Object> getErrorResponseParameters(Event failureEvent) {
        return getParametersTransformer()
            .map(parametersTransformer -> parametersTransformer.fromMessageToErrorResponseParameters(event.getMessage()))
            .orElseGet(() -> getParametersProcessor().getFailedExecutionResponseParametersFunction().apply(event));
      }
    };
  }

}
