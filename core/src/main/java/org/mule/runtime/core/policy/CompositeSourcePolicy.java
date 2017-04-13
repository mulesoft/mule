/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  private final Processor flowExecutionProcessor;
  private final SourcePolicyProcessorFactory sourcePolicyProcessorFactory;
  private Map<String, Object> originalResponseParameters;
  private Map<String, Object> originalFailureResponseParameters;
  private Event flowExecutionResponse;

  /**
   * Creates a new source policies composed by several {@link Policy} that will be chain together.
   *
   * @param parameterizedPolicies the list of policies to use in this composite policy.
   * @param sourcePolicyParametersTransformer a transformer from a source response parameters to a message and vice versa
   * @param sourcePolicyProcessorFactory factory to create a {@link Processor} from each {@link Policy}
   * @param flowExecutionProcessor the operation that executes the flow
   * @param messageSourceResponseParametersProcessor processor that gives access to the set of parameters to be sent originally by
   *        the source
   */
  public CompositeSourcePolicy(List<Policy> parameterizedPolicies,
                               Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer,
                               SourcePolicyProcessorFactory sourcePolicyProcessorFactory, Processor flowExecutionProcessor,
                               MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {
    super(parameterizedPolicies, sourcePolicyParametersTransformer, messageSourceResponseParametersProcessor);
    this.sourcePolicyProcessorFactory = sourcePolicyProcessorFactory;
    this.flowExecutionProcessor = flowExecutionProcessor;
  }

  /**
   * Executes the flow.
   * 
   * If there's a {@link SourcePolicyParametersTransformer} provided then it will use it to convert the source response or source
   * failure response from the parameters back to a {@link Message} that can be routed through the policy chain which later will
   * be convert back to response or failure response parameters thus allowing the policy chain to modify the response.. That
   * message will be the result of the next-operation of the policy.
   * 
   * If no {@link SourcePolicyParametersTransformer} is provided, then the same response from the flow is going to be routed as
   * response of the next-operation of the policy chain. In this case, the same response from the flow is going to be used to
   * generate the response or failure response for the source so the policy chain is not going to be able to modify the response
   * sent by the source.
   * 
   * When the flow execution fails, it will create a {@link FlowExecutionException} instead of a regular
   * {@link MessagingException} to signal that the failure was through the the flow exception and not the policy logic.
   */
  @Override
  protected Event processNextOperation(Event event) throws MuleException {
    try {
      flowExecutionResponse = flowExecutionProcessor.process(event);
      originalResponseParameters =
          getParametersProcessor().getSuccessfulExecutionResponseParametersFunction().apply(flowExecutionResponse);
      Message message = getParametersTransformer()
          .map(parametersTransformer -> parametersTransformer.fromSuccessResponseParametersToMessage(originalResponseParameters))
          .orElseGet(flowExecutionResponse::getMessage);
      return Event.builder(event).message(message).build();
    } catch (MessagingException messagingException) {
      originalFailureResponseParameters =
          getParametersProcessor().getFailedExecutionResponseParametersFunction().apply(messagingException.getEvent());
      Message message = getParametersTransformer()
          .map(parametersTransformer -> parametersTransformer
              .fromFailureResponseParametersToMessage(originalFailureResponseParameters))
          .orElse(messagingException.getEvent().getMessage());
      throw new FlowExecutionException(Event.builder(event).message(message).build(),
                                       messagingException.getCause(),
                                       messagingException.getFailingMessageProcessor());
    }
  }

  /**
   * Always return the policy execution / flow execution result so the next policy executes with the modified version of the
   * wrapped policy / flow.
   */
  @Override
  protected Event processPolicy(Policy policy, Processor nextProcessor, Event event)
      throws Exception {
    Processor defaultSourcePolicy =
        sourcePolicyProcessorFactory.createSourcePolicy(policy, nextProcessor);
    return defaultSourcePolicy.process(event);
  }

  /**
   * Process the set of policies.
   * 
   * When there's a {@link SourcePolicyParametersTransformer} then the final set of parameters to be sent by the response function
   * and the error response function will be calculated based on the output of the policy chain. If there's no
   * {@link SourcePolicyParametersTransformer} then those parameters will be exactly the one defined by the message source.
   * 
   * @param sourceEvent the event generated from the source.
   * @return a {@link SuccessSourcePolicyResult} which contains the response parameters and the result event of the execution or a
   *         {@link FailureSourcePolicyResult} which contains the failure response parameters and the {@link MessagingException}
   *         thrown by the policy chain execution.
   * @throws Exception if there was an unexpected failure thrown by executing the chain.
   */
  @Override
  public Either<FailureSourcePolicyResult, SuccessSourcePolicyResult> process(Event sourceEvent) throws Exception {
    try {
      Event policiesResultEvent = processPolicies(sourceEvent);
      Map<String, Object> responseParameters =
          getParametersTransformer().map(parametersTransformer -> concatMaps(originalResponseParameters, parametersTransformer
              .fromMessageToSuccessResponseParameters(policiesResultEvent.getMessage()))).orElse(originalResponseParameters);
      return right(new SuccessSourcePolicyResult(policiesResultEvent, responseParameters, getParametersProcessor()));
    } catch (FlowExecutionException e) {
      Map<String, Object> responseParameters =
          getParametersTransformer()
              .map(parametersTransformer -> concatMaps(originalFailureResponseParameters, parametersTransformer
                  .fromMessageToErrorResponseParameters(e.getEvent().getMessage())))
              .orElse(originalFailureResponseParameters);
      return left(new FailureSourcePolicyResult(e, responseParameters));
    } catch (MessagingException e) {
      Map<String, Object> responseParameters =
          getParametersTransformer()
              .map(parametersTransformer -> concatMaps(originalFailureResponseParameters, parametersTransformer
                  .fromMessageToErrorResponseParameters(of(null))))
              .orElse(originalFailureResponseParameters);
      return left(new FailureSourcePolicyResult(e, responseParameters));
    }
  }

  private Map<String, Object> concatMaps(Map<String, Object> originalResponseParameters,
                                         Map<String, Object> policyResponseParameters) {
    Map<String, Object> concatMap = new HashMap<>();
    if (originalResponseParameters != null) {
      concatMap.putAll(originalResponseParameters);
    }
    concatMap.putAll(policyResponseParameters);
    return concatMap;
  }

}
