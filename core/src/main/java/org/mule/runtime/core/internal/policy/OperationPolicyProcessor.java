/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor.POLICY_NEXT_OPERATION;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import org.reactivestreams.Publisher;

/**
 * This class is responsible for the processing of a policy applied to a {@link Processor}. Currently the only kind of
 * {@link Processor} supported is an operation from the SDK.
 * <p>
 * In order for this class to be able to execute a policy it requires an {@link PolicyChain} with the content of the policy. Such
 * policy may have an {@link PolicyNextActionMessageProcessor} which will be the one used to execute the provided
 * {@link Processor}.
 * <p>
 * This class enforces the scoping of variables between the actual behaviour and the policy that may be applied to it. To enforce
 * such scoping of variables it uses internal parameters so the last {@link CoreEvent} modified by the policy behaviour can be
 * stored and retrieve for later usages. It also uses {@link PolicyEventConverter} as a helper class to convert an
 * {@link CoreEvent} from the policy to the next operation {@link CoreEvent} or from the next operation result to the
 * {@link CoreEvent} that must continue the execution of the policy.
 * <p>
 */
public class OperationPolicyProcessor implements ReactiveProcessor {

  private final Policy policy;
  private final Reference<ReactiveProcessor> nextProcessorRef;
  private final PolicyEventMapper policyEventMapper;
  private final PolicyTraceLogger policyTraceLogger;

  public OperationPolicyProcessor(Policy policy, ReactiveProcessor nextProcessor) {
    this.policy = policy;
    this.nextProcessorRef = new WeakReference<>(nextProcessor);
    this.policyEventMapper = new PolicyEventMapper(policy.getPolicyId());
    this.policyTraceLogger = new PolicyTraceLogger();
  }

  /**
   * Process the policy chain of processors. The provided {@code nextOperation} function has the behaviour to be executed by the
   * next-operation of the chain.
   *
   * @return the result of processing the {@code event} through the policy chain.
   */
  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher)
        .map(policyEventMapper::onOperationPolicyBegin)
        .doOnNext(event -> policyTraceLogger.logOperationPolicyStart(policy, event))
        .transform(policy.getPolicyChain().onChainError(t -> manageError((MessagingException) t)))
        .subscriberContext(ctx -> ctx.put(POLICY_NEXT_OPERATION, nextProcessorRef))
        .doOnNext(event -> policyTraceLogger.logOperationPolicyEnd(policy, event))
        .map(policyChainResult -> policyEventMapper
            .onOperationPolicyFinish(policyChainResult, policy.getPolicyChain().isPropagateMessageTransformations()));
  }

  private void manageError(MessagingException messagingException) {
    messagingException.setProcessedEvent(policyEventMapper.onOperationPolicyError(messagingException.getEvent()));
  }
}
