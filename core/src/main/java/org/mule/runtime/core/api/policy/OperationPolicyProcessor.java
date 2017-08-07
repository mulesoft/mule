/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.policy;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.policy.PolicyEventConverter;

import java.util.Optional;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * This class is responsible for the processing of a policy applied to a {@link Processor}. Currently the only kind of
 * {@link Processor} supported is an operation from the SDK.
 * <p>
 * In order for this class to be able to execute a policy it requires an {@link PolicyChain} with the content of the policy. Such
 * policy may have an {@link PolicyNextActionMessageProcessor} which will be the one used to execute the provided
 * {@link Processor}.
 * <p>
 * This class enforces the scoping of variables between the actual behaviour and the policy that may be applied to it. To enforce
 * such scoping of variables it uses {@link PolicyStateHandler} so the last {@link Event} modified by the policy behaviour can be
 * stored and retrieve for later usages. It also uses {@link PolicyEventConverter} as a helper class to convert an {@link Event}
 * from the policy to the next operation {@link Event} or from the next operation result to the {@link Event} that must continue
 * the execution of the policy.
 * <p>
 */
public class OperationPolicyProcessor implements Processor {

  private final Policy policy;
  private final PolicyStateHandler policyStateHandler;
  private final PolicyEventConverter policyEventConverter = new PolicyEventConverter();
  private final Processor nextProcessor;

  public OperationPolicyProcessor(Policy policy,
                                  PolicyStateHandler policyStateHandler,
                                  Processor nextProcessor) {
    this.policy = policy;
    this.policyStateHandler = policyStateHandler;
    this.nextProcessor = nextProcessor;
  }

  /**
   * Process the policy chain of processors. The provided {@code nextOperation} function has the behaviour to be executed by the
   * next-operation of the chain.
   *
   * @param operationEvent the event with the data to execute the operation
   * @return the result of processing the {@code event} through the policy chain.
   * @throws MuleException
   */
  @Override
  public Event process(Event operationEvent) throws MuleException {
    return processToApply(operationEvent, this);
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    return from(publisher).then(operationEvent -> {
      PolicyStateId policyStateId = new PolicyStateId(operationEvent.getContext().getCorrelationId(), policy.getPolicyId());
      Optional<Event> latestPolicyState = policyStateHandler.getLatestState(policyStateId);
      Event variablesProviderEvent =
          latestPolicyState.orElseGet(() -> Event.builder(operationEvent.getInternalContext()).message(of(null)).build());
      policyStateHandler.updateState(policyStateId, variablesProviderEvent);
      Event policyEvent = policyEventConverter.createEvent(operationEvent, variablesProviderEvent);
      Processor operationCall = buildOperationExecutionWithPolicyFunction(nextProcessor, operationEvent);
      policyStateHandler.updateNextOperation(policyStateId.getExecutionIdentifier(), operationCall);
      return executePolicyChain(operationEvent, policyStateId, policyEvent);
    });
  }

  private Mono<Event> executePolicyChain(Event operationEvent, PolicyStateId policyStateId, Event policyEvent) {
    return just(policyEvent).transform(policy.getPolicyChain())
        .doOnNext(policyChainResult -> policyStateHandler.updateState(policyStateId, policyChainResult))
        .map(policyChainResult -> policyEventConverter.createEvent(policyChainResult, operationEvent));
  }

  private Processor buildOperationExecutionWithPolicyFunction(Processor nextOperation, Event operationEvent) {
    return new Processor() {

      @Override
      public Event process(Event event) throws MuleException {
        return processToApply(event, this);
      }

      @Override
      public Publisher<Event> apply(Publisher<Event> publisher) {
        return from(publisher).then(policyExecuteNextEvent -> {
          PolicyStateId policyStateId = new PolicyStateId(policyExecuteNextEvent.getContext().getId(), policy.getPolicyId());
          policyStateHandler.updateState(policyStateId, policyExecuteNextEvent);
          return just(policyExecuteNextEvent).map(event -> policyEventConverter.createEvent(event, operationEvent))
              .transform(nextOperation)
              .map(operationResult -> policyEventConverter.createEvent(operationResult, policyExecuteNextEvent));
        });
      }
    };
  }

}
