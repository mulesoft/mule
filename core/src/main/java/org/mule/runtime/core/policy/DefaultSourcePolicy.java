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
import org.mule.runtime.core.exception.MessagingException;

import java.util.Map;
import java.util.Optional;

public class DefaultSourcePolicy implements SourcePolicy {

  private final AbstractPolicyChain policyChain;
  private final Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer;
  private final PolicyStateHandler policyStateHandler;

  public DefaultSourcePolicy(AbstractPolicyChain abstractPolicyChain,
                             Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer,
                             PolicyStateHandler policyStateHandler) {
    this.policyChain = abstractPolicyChain;
    this.sourcePolicyParametersTransformer = sourcePolicyParametersTransformer;
    this.policyStateHandler = policyStateHandler;
  }

  @Override
  public Event process(Event sourceEvent, NextOperation nextOperation,
                       MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor)
      throws Exception {
    NextOperation sourceNextOperation =
        buildFlowExecutionWithPolicyFunction(nextOperation, sourceEvent, messageSourceResponseParametersProcessor);
    policyStateHandler.updateNextOperation(sourceEvent.getContext().getId(), sourceNextOperation);
    return policyChain.execute(sourceEvent);
  }

  private NextOperation buildFlowExecutionWithPolicyFunction(NextOperation nextOperation, Event sourceEvent,
                                                             MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {
    return (processEvent) -> {
      try {
        Event flowExecutionResponse = nextOperation.execute(sourceEvent);
        if (sourcePolicyParametersTransformer.isPresent()) {
          Map<String, Object> responseParameters =
              messageSourceResponseParametersProcessor.getSuccessfulExecutionResponseParametersFunction()
                  .apply(flowExecutionResponse);
          return Event.builder(processEvent.getContext())
              .message((InternalMessage) sourcePolicyParametersTransformer.get()
                  .fromSuccessResponseParametersToMessage(responseParameters))
              .build();
        } else {
          return Event.builder(flowExecutionResponse).build();
        }
      } catch (MessagingException messagingException) {
        Map<String, Object> failureParameters =
            messageSourceResponseParametersProcessor.getFailedExecutionResponseParametersFunction()
                .apply(messagingException.getEvent());
        if (sourcePolicyParametersTransformer.isPresent()) {
          Message message = sourcePolicyParametersTransformer.get().fromFailureResponseParametersToMessage(failureParameters);
          Event.Builder eventBuilder =
              Event.builder(messagingException.getEvent().getContext()).message((InternalMessage) message);
          Event policyLatestState = policyStateHandler.getLatestState(sourceEvent.getContext().getId()).get();
          policyLatestState.getVariableNames().forEach(variableName -> {
            eventBuilder.addVariable(variableName, policyLatestState.getVariable(variableName));
          });
          throw new MessagingException(eventBuilder.build(), messagingException.getCause());
        }
        throw messagingException;
      }
    };
  }
}
