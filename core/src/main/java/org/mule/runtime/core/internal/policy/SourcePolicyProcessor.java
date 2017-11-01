/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.core.api.policy.PolicyNextActionMessageProcessor;
import org.mule.runtime.core.api.policy.PolicyStateHandler;
import org.mule.runtime.core.api.policy.PolicyStateId;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import java.util.Optional;

import org.reactivestreams.Publisher;

/**
 * This class is responsible for the processing of a policy applied to a {@link org.mule.runtime.core.api.source.MessageSource}.
 *
 * In order for this class to be able to execute a policy it requires an {@link PolicyChain} with the content of the policy. Such
 * policy may have an {@link PolicyNextActionMessageProcessor} which will be the one used to execute the provided
 * {@link Processor} which may be another policy or the actual logic behind the
 * {@link org.mule.runtime.core.api.source.MessageSource} which typically is a flow execution.
 *
 * This class enforces the scoping of variables between the actual behaviour and the policy that may be applied to it. To enforce
 * such scoping of variables it uses {@link PolicyStateHandler} so the last {@link CoreEvent} modified by the policy behaviour can
 * be stored and retrieve for later usages. It also uses {@link PolicyEventConverter} as a helper class to convert an
 * {@link CoreEvent} from the policy to the next operation {@link CoreEvent} or from the next operation result to the
 * {@link CoreEvent} that must continue the execution of the policy.
 *
 * If a non-empty {@code sourcePolicyParametersTransformer} is passed to this class, then it will be used to convert the result of
 * the policy chain execution to the set of parameters that the success response function or the failure response function will be
 * used to execute.
 */
public class SourcePolicyProcessor implements Processor {

  private final Policy policy;
  private final PolicyStateHandler policyStateHandler;
  private final PolicyEventConverter policyEventConverter = new PolicyEventConverter();
  private final Processor nextProcessor;

  /**
   * Creates a new {@code DefaultSourcePolicy}.
   *
   * @param policy the policy to execute before and after the source.
   * @param policyStateHandler the state handler for the policy.
   * @param nextProcessor the next-operation processor implementation, it may be another policy or the flow execution.
   */
  public SourcePolicyProcessor(Policy policy,
                               PolicyStateHandler policyStateHandler, Processor nextProcessor) {
    this.policy = policy;
    this.policyStateHandler = policyStateHandler;
    this.nextProcessor = nextProcessor;
  }

  /**
   * Process the source policy chain of processors. The provided {@code nextOperation} function has the behaviour to be executed
   * by the next-operation of the chain which may be the next policy in the chain or the flow execution.
   *
   * @param sourceEvent the event with the data created from the source message that must be used to execute the source policy.
   * @return the result of processing the {@code event} through the policy chain.
   * @throws MuleException
   */
  @Override
  public CoreEvent process(CoreEvent sourceEvent) throws MuleException {
    return processToApply(sourceEvent, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher)
        .cast(PrivilegedEvent.class)
        .flatMap(sourceEvent -> {
          String executionIdentifier = sourceEvent.getContext().getCorrelationId();
          policyStateHandler.updateNextOperation(executionIdentifier,
                                                 buildSourceExecutionWithPolicyFunction(executionIdentifier, sourceEvent));
          return just(sourceEvent)
              .map(event -> policyEventConverter.createEvent(sourceEvent,
                                                             PrivilegedEvent.builder(sourceEvent.getContext()).message(of(null))
                                                                 .build()))
              .cast(CoreEvent.class)
              .transform(policy.getPolicyChain())
              .cast(PrivilegedEvent.class)
              .map(event -> policyEventConverter.createEvent(event, sourceEvent));
        });
  }

  private Processor buildSourceExecutionWithPolicyFunction(String executionIdentifier, PrivilegedEvent sourceEvent) {
    return new Processor() {

      @Override
      public CoreEvent process(CoreEvent event) throws MuleException {
        return processToApply(event, this);
      }

      @Override
      public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
        return from(publisher)
            .cast(PrivilegedEvent.class)
            .doOnNext(event -> saveState(event))
            .map(event -> (CoreEvent) policyEventConverter.createEvent(event, sourceEvent))
            .flatMap(e -> from(processWithChildContext(e, nextProcessor, Optional.empty()))
                .cast(PrivilegedEvent.class))
            .map(result -> policyEventConverter.createEvent(result, loadState()))
            .onErrorMap(MessagingException.class,
                        e -> new MessagingException(policyEventConverter.createEvent((PrivilegedEvent) e.getEvent(), loadState()),
                                                    e))
            .cast(CoreEvent.class);
      }

      private PolicyStateId policyStateId = new PolicyStateId(executionIdentifier, policy.getPolicyId());

      private void saveState(PrivilegedEvent event) {
        policyStateHandler.updateState(policyStateId, event);
      }

      private PrivilegedEvent loadState() {
        return (PrivilegedEvent) policyStateHandler.getLatestState(policyStateId).get();
      }
    };
  }
}
