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
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;

import java.util.Map;
import java.util.Optional;

/**
 * This class is responsible for the processing of a policy applied to a {@link NextOperation}. Currently the only kind of
 * {@link NextOperation} supported is an operation from the SDK.
 * <p>
 * In order for this class to be able to execute a policy it requires an {@link PolicyChain} with the content of the policy. Such
 * policy may have an {@link PolicyNextActionMessageProcessor} which will be the one used to execute the provided
 * {@link NextOperation}.
 * <p>
 * This class enforces the scoping of variables between the actual behaviour and the policy that may be applied to it. To enforce
 * such scoping of variables it uses {@link PolicyStateHandler} so the last {@link Event} modified by the policy behaviour can be
 * stored and retrieve for later usages. It also uses {@link PolicyEventConverter} as a helper class to convert an {@link Event}
 * from the policy to the next operation {@link Event} or from the next operation result to the {@link Event} that must continue
 * the execution of the policy.
 * <p>
 * If a non-empty {@code operationPolicyParametersTransformer} is passed to this class, then it will be used to convert the
 * message that arrives to the next-operation component of the policy chain to a new {@link Event} containing the data to be used
 * to execute the provided {@link NextOperation}.
 */
public class OperationPolicy {

  private final PolicyChain policyChain;
  private final Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer;
  private final PolicyStateHandler policyStateHandler;
  private final PolicyEventConverter policyEventConverter = new PolicyEventConverter();

  public OperationPolicy(PolicyChain policyChain,
                         Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer,
                         PolicyStateHandler policyStateHandler) {
    this.policyChain = policyChain;
    this.operationPolicyParametersTransformer = operationPolicyParametersTransformer;
    this.policyStateHandler = policyStateHandler;
  }

  /**
   * Process the policy chain of processors. The provided {@code nextOperation} function has the behaviour to be executed by the
   * next-operation of the chain.
   *
   * @param event the event with the data to execute the policy
   * @param nextOperation the next-operation processor implementation
   * @param operationParametersProcessor a processor that converts an {@link Event} to the set of parameters to be sent by the
   *        operation based on the user configuration.
   * @return the result of processing the {@code event} through the policy chain.
   * @throws Exception
   */
  public Event process(Event sourceEvent, NextOperation nextOperation, OperationParametersProcessor operationParametersProcessor)
      throws Exception {
    Optional<Event> latestPolicyState = policyStateHandler.getLatestState(sourceEvent.getContext().getId());
    Event variablesProviderEvent = latestPolicyState.orElseGet(() -> Event.builder(sourceEvent.getContext()).build());
    Map<String, Object> originalParametersMap = operationParametersProcessor.getOperationParameters();
    Message message = sourceEvent.getMessage();
    message = operationPolicyParametersTransformer
        .map(parametersTransformer -> parametersTransformer.fromParametersToMessage(originalParametersMap)).orElse(message);
    Event policyEvent = policyEventConverter.createEvent(message, variablesProviderEvent);
    NextOperation operationCall = buildOperationExecutionWithPolicyFunction(nextOperation);
    policyStateHandler.updateNextOperation(sourceEvent.getContext().getId(), operationCall);
    Event policyChainResult = policyChain.execute(policyEvent);
    policyStateHandler.updateState(sourceEvent.getContext().getId(), policyChainResult);
    return policyEventConverter.createEvent(policyChainResult.getMessage(), sourceEvent);
  }

  private NextOperation buildOperationExecutionWithPolicyFunction(NextOperation nextOperation)
      throws Exception {
    return policyExecuteNextEvent -> {
      try {
        policyStateHandler.updateState(policyExecuteNextEvent.getContext().getId(), policyExecuteNextEvent);
        Event operationResult = nextOperation.execute(policyExecuteNextEvent);
        return policyEventConverter.createEvent(operationResult.getMessage(), policyExecuteNextEvent);
      } catch (MuleException e) {
        throw new MuleRuntimeException(e);
      }
    };
  }

}
