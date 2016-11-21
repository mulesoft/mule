/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;

import java.util.Map;
import java.util.Optional;

/**
 * This class is responsible for the processing of a policy applied to a {@link org.mule.runtime.core.api.source.MessageSource}.
 * 
 * In order for this class to be able to execute a policy it requires an {@link PolicyChain} with the content of the policy. Such
 * policy may have an {@link PolicyNextActionMessageProcessor} which will be the one used to execute the provided
 * {@link Processor} which may be another policy or the actual logic behind the
 * {@link org.mule.runtime.core.api.source.MessageSource} which typically is a flow execution.
 * 
 * This class enforces the scoping of variables between the actual behaviour and the policy that may be applied to it. To enforce
 * such scoping of variables it uses {@link PolicyStateHandler} so the last {@link Event} modified by the policy behaviour can be
 * stored and retrieve for later usages. It also uses {@link PolicyEventConverter} as a helper class to convert an {@link Event}
 * from the policy to the next operation {@link Event} or from the next operation result to the {@link Event} that must continue
 * the execution of the policy.
 * 
 * If a non-empty {@code sourcePolicyParametersTransformer} is passed to this class, then it will be used to convert the result of
 * the policy chain execution to the set of parameters that the success response function or the failure response function will be
 * used to execute.
 */
public class SourcePolicy {

  private final PolicyChain policyChain;
  private final Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer;
  private final PolicyStateHandler policyStateHandler;
  private final PolicyEventConverter policyEventConverter = new PolicyEventConverter();

  public SourcePolicy(PolicyChain policyChain,
                      Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer,
                      PolicyStateHandler policyStateHandler) {
    this.policyChain = policyChain;
    this.sourcePolicyParametersTransformer = sourcePolicyParametersTransformer;
    this.policyStateHandler = policyStateHandler;
  }

  /**
   * Process the source policy chain of processors. The provided {@code nextOperation} function has the behaviour to be executed
   * by the next-operation of the chain which may be the next policy in the chain or the flow execution.
   *
   *
   * @param sourceEvent the event with the data created from the source message that must be used to execute the source policy.
   * @param nextOperation the next-operation processor implementation, it may be another policy or the flow execution.
   * @param messageSourceResponseParametersProcessor a processor to convert an {@link Event} to the set of parameters used to
   *        execute the successful or failure response function of the source.
   * @return the result of processing the {@code event} through the policy chain.
   * @throws Exception
   */
  public Event process(Event sourceEvent, Processor nextOperation,
                       MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor)
      throws Exception {
    Processor sourceNextOperation =
        buildFlowExecutionWithPolicyFunction(nextOperation, sourceEvent, messageSourceResponseParametersProcessor);
    policyStateHandler.updateNextOperation(sourceEvent.getContext().getId(), sourceNextOperation);
    Event result = policyChain
        .process(policyEventConverter.createEvent(sourceEvent.getMessage(), Event.builder(sourceEvent.getContext()).build()));
    return Event.builder(result.getContext()).message(result.getMessage()).build();
  }

  private Processor buildFlowExecutionWithPolicyFunction(Processor nextOperation, Event sourceEvent,
                                                         MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {
    return (processEvent) -> {
      Event lastPolicyEvent = policyStateHandler.getLatestState(sourceEvent.getContext().getId()).get();
      try {
        Event flowExecutionResponse = nextOperation.process(sourceEvent);
        Message message = sourcePolicyParametersTransformer.map(policyTransformer -> {
          Map<String, Object> responseParameters =
              messageSourceResponseParametersProcessor.getSuccessfulExecutionResponseParametersFunction()
                  .apply(flowExecutionResponse);
          return sourcePolicyParametersTransformer.get()
              .fromSuccessResponseParametersToMessage(responseParameters);
        }).orElseGet(flowExecutionResponse::getMessage);
        return policyEventConverter.createEvent(message, lastPolicyEvent);
      } catch (MessagingException messagingException) {
        Message message = messagingException.getEvent().getMessage();
        Map<String, Object> failureParameters =
            messageSourceResponseParametersProcessor.getFailedExecutionResponseParametersFunction()
                .apply(messagingException.getEvent());
        if (sourcePolicyParametersTransformer.isPresent()) {
          message = sourcePolicyParametersTransformer.get().fromFailureResponseParametersToMessage(failureParameters);
          throw new MessagingException(Event.builder(lastPolicyEvent).message((InternalMessage) message).build(),
                                       messagingException.getCause());
        }
        throw messagingException;
      }
    };
  }
}
