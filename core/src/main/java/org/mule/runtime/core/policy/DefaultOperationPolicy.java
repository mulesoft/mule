/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultOperationPolicy implements OperationPolicy {

  private final AbstractPolicyChain policyChain;
  private final Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer;
  private final PolicyStateHandler policyStateHandler;

  public DefaultOperationPolicy(AbstractPolicyChain abstractPolicyChain,
                                Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer,
                                PolicyStateHandler policyStateHandler) {
    this.policyChain = abstractPolicyChain;
    this.operationPolicyParametersTransformer = operationPolicyParametersTransformer;
    this.policyStateHandler = policyStateHandler;
  }

  @Override
  public Event process(Event sourceEvent, NextOperation nextOperation, OperationParametersProcessor operationParametersProcessor)
      throws Exception {
    NextOperation sourceNextOperation =
        buildOperationExecutionWithPolicyFunction(nextOperation, sourceEvent, operationParametersProcessor);
    policyStateHandler.updateNextOperation(sourceEvent.getContext().getId(), sourceNextOperation);
    return policyChain.execute(sourceEvent);
  }

  private NextOperation buildOperationExecutionWithPolicyFunction(NextOperation nextOperation, Event sourceEvent,
                                                                  OperationParametersProcessor operationParametersProcessor)
      throws Exception {
    Event policyEvent;
    Optional<Event> latestPolicyStateState = policyStateHandler.getLatestState(sourceEvent.getContext().getId());
    Event.Builder policyEventBuilder = latestPolicyStateState.isPresent() ? Event.builder(latestPolicyStateState.get())
        : Event.builder(sourceEvent.getContext());
    Map<String, Object> originalParametersMap = operationParametersProcessor.getOperationParameters();
    if (operationPolicyParametersTransformer.isPresent()) {
      Message message = operationPolicyParametersTransformer.get().fromParametersToMessage(originalParametersMap);
      policyEvent = policyEventBuilder.message((InternalMessage) message).build();
    } else {
      policyEvent = policyEventBuilder.message(sourceEvent.getMessage()).build();
    }
    AtomicReference<Event> operationResult = new AtomicReference<>();
    return policyExecuteNextEvent -> {
      policyStateHandler.updateState(policyExecuteNextEvent.getContext().getId(), policyExecuteNextEvent);
      try {
        Map<String, Object> parameters = new HashMap<>();
        parameters.putAll(originalParametersMap);
        if (operationPolicyParametersTransformer.isPresent()) {
          parameters.putAll(operationPolicyParametersTransformer.get()
              .fromMessageToParameters(policyExecuteNextEvent.getMessage()));
        }
        return nextOperation.execute(policyEvent);
      } catch (MuleException e) {
        throw new MuleRuntimeException(e);
      }
    };
  }

}
