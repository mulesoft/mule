/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.core.api.policy.PolicyStateHandler;
import org.mule.runtime.core.api.policy.PolicyStateId;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.Supplier;

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
 * such scoping of variables it uses {@link PolicyStateHandler} so the last {@link CoreEvent} modified by the policy behaviour can
 * be stored and retrieve for later usages. It also uses {@link PolicyEventConverter} as a helper class to convert an
 * {@link CoreEvent} from the policy to the next operation {@link CoreEvent} or from the next operation result to the
 * {@link CoreEvent} that must continue the execution of the policy.
 * <p>
 */
public class OperationPolicyProcessor implements Processor {

  private static final Logger LOGGER = getLogger(OperationPolicyProcessor.class);

  private final Policy policy;
  private final PolicyStateHandler policyStateHandler;
  private final PolicyEventConverter policyEventConverter = new PolicyEventConverter();
  private final ReactiveProcessor nextProcessor;
  private final PolicyStateIdFactory stateIdFactory;

  public OperationPolicyProcessor(Policy policy,
                                  PolicyStateHandler policyStateHandler,
                                  ReactiveProcessor nextProcessor) {
    this.policy = policy;
    this.policyStateHandler = policyStateHandler;
    this.nextProcessor = nextProcessor;
    this.stateIdFactory = new PolicyStateIdFactory(policy.getPolicyId());
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
  public CoreEvent process(CoreEvent operationEvent) throws MuleException {
    return processToApply(operationEvent, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher)
        .cast(PrivilegedEvent.class)
        .flatMap(operationEvent -> {
          PolicyStateId policyStateId = stateIdFactory.create(operationEvent);
          PrivilegedEvent variablesProviderEvent = variablesProvider(operationEvent, policyStateId);
          PrivilegedEvent policyEvent = policyEventConverter.createEvent(operationEvent, variablesProviderEvent);
          Processor operationCall = buildOperationExecutionWithPolicyFunction(nextProcessor, operationEvent, policyStateId);
          policyStateHandler.updateNextOperation(policyStateId.getExecutionIdentifier(), operationCall);
          return executePolicyChain(operationEvent, policyStateId, policyEvent);
        });
  }

  private void manageError(PolicyStateId policyStateId, PrivilegedEvent operationEvent, MessagingException messagingException) {
    policyStateHandler.updateState(policyStateId, messagingException.getEvent());
    PrivilegedEvent newEvent = policyEventConverter.createEvent((PrivilegedEvent) messagingException.getEvent(), operationEvent);
    messagingException.setProcessedEvent(newEvent);
  }

  private Mono<PrivilegedEvent> executePolicyChain(PrivilegedEvent operationEvent, PolicyStateId policyStateId,
                                                   PrivilegedEvent policyEvent) {

    PolicyChain policyChain = policy.getPolicyChain();
    policyChain.onChainError(t -> manageError(policyStateId, operationEvent, (MessagingException) t));

    return just(policyEvent)
        .doOnNext(event -> logPolicy(event.getContext().getCorrelationId(), policyStateId.getPolicyId(),
                                     () -> getMessageAttributesAsString(event), "Before operation"))
        .cast(CoreEvent.class)
        .transform(policyChain)
        .cast(PrivilegedEvent.class)
        .doOnNext(policyChainResult -> policyStateHandler.updateState(policyStateId, policyChainResult))
        .map(policyChainResult -> policyEventConverter.createEvent(policyChainResult, operationEvent))
        .doOnNext(event -> logPolicy(event.getContext().getCorrelationId(), policyStateId.getPolicyId(),
                                     () -> getMessageAttributesAsString(event), "After operation"));
  }

  private Processor buildOperationExecutionWithPolicyFunction(ReactiveProcessor nextOperation, PrivilegedEvent operationEvent,
                                                              PolicyStateId policyStateId) {
    return new Processor() {

      @Override
      public CoreEvent process(CoreEvent event) throws MuleException {
        return processToApply(event, this);
      }

      @Override
      public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
        return from(publisher)
            .cast(PrivilegedEvent.class)
            .flatMap(policyExecuteNextEvent -> {
              policyStateHandler.updateState(policyStateId, policyExecuteNextEvent);
              return just(policyExecuteNextEvent)
                  .map(event -> policyEventConverter.createEvent(event, operationEvent))
                  .cast(CoreEvent.class)
                  .transform(nextOperation)
                  .cast(PrivilegedEvent.class)
                  .map(operationResult -> policyEventConverter.createEvent(operationResult, policyExecuteNextEvent));
            });
      }
    };
  }

  private PrivilegedEvent variablesProvider(CoreEvent event, PolicyStateId policyStateId) {
    Optional<CoreEvent> latestPolicyState = policyStateHandler.getLatestState(policyStateId);
    return (PrivilegedEvent) latestPolicyState
        .orElseGet(() -> PrivilegedEvent.builder(event.getContext()).message(of(null)).build());
  }

  private String getMessageAttributesAsString(CoreEvent event) {
    if (event.getMessage() == null || event.getMessage().getAttributes() == null
        || event.getMessage().getAttributes().getValue() == null) {
      return "";
    }
    return event.getMessage().getAttributes().getValue().toString();
  }

  private void logPolicy(String eventId, String policyName, Supplier<String> message, String startingMessage) {
    if (LOGGER.isTraceEnabled()) {
      // TODO Remove event id when first policy generates it. MULE-14455
      LOGGER.trace("Event Id: " + eventId + ".\n" + startingMessage + "\nPolicy:" + policyName + "\n" + message.get());
    }
  }
}
