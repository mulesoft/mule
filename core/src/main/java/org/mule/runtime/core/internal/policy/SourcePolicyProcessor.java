/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor.POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS;
import static org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor.POLICY_NEXT_OPERATION;
import static org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor.POLICY_VARS;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import org.reactivestreams.Publisher;

import java.util.Map;
import java.util.Map.Entry;

/**
 * This class is responsible for the processing of a policy applied to a {@link org.mule.runtime.core.api.source.MessageSource}.
 * <p>
 * In order for this class to be able to execute a policy it requires an {@link PolicyChain} with the content of the policy. Such
 * policy may have an {@link PolicyNextActionMessageProcessor} which will be the one used to execute the provided
 * {@link Processor} which may be another policy or the actual logic behind the
 * {@link org.mule.runtime.core.api.source.MessageSource} which typically is a flow execution.
 * <p>
 * This class enforces the scoping of variables between the actual behaviour and the policy that may be applied to it. To enforce
 * such scoping of variables it uses internal parameters so the last {@link CoreEvent} modified by the policy behaviour can be
 * stored and retrieve for later usages. It also uses {@link PolicyEventConverter} as a helper class to convert an
 * {@link CoreEvent} from the policy to the next operation {@link CoreEvent} or from the next operation result to the
 * {@link CoreEvent} that must continue the execution of the policy.
 * <p/>
 * If a non-empty {@code sourcePolicyParametersTransformer} is passed to this class, then it will be used to convert the result of
 * the policy chain execution to the set of parameters that the success response function or the failure response function will be
 * used to execute.
 */
public class SourcePolicyProcessor implements ReactiveProcessor {

  public static final String POLICY_SOURCE_ORIGINAL_EVENT = "policy.source.originalEvent";

  private final Policy policy;
  private final PolicyEventConverter policyEventConverter = new PolicyEventConverter();
  private final ReactiveProcessor nextProcessor;

  /**
   * Creates a new {@code DefaultSourcePolicy}.
   *
   * @param policy the policy to execute before and after the source.
   * @param nextProcessor the next-operation processor implementation, it may be another policy or the flow execution.
   */
  public SourcePolicyProcessor(Policy policy, ReactiveProcessor nextProcessor) {
    this.policy = policy;
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
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher)
        .cast(PrivilegedEvent.class)
        .map(sourceEvent -> InternalEvent
            .builder(sourceEvent)
            .addInternalParameter(POLICY_SOURCE_ORIGINAL_EVENT, sourceEvent)
            .clearVariables().build())
        .cast(CoreEvent.class)
        .transform(policy.getPolicyChain().onChainError(t -> {
          MessagingException me = (MessagingException) t;

          String policyVarsInternalParameterName = policyVarsInternalParameterName();
          for (Entry<String, ?> entry : ((InternalEvent) me.getEvent()).getInternalParameters().entrySet()) {
            if (policyVarsInternalParameterName.equals(entry.getKey())) {
              me.setProcessedEvent(policyEventConverter.restoreVariables((PrivilegedEvent) me.getEvent(),
                                                                         (Map<String, TypedValue<?>>) entry.getValue()));
              break;
            }
          }
        }))
        .subscriberContext(ctx -> ctx.put(POLICY_NEXT_OPERATION, nextProcessor)
            .put(POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS, policy.getPolicyChain().isPropagateMessageTransformations()))
        .cast(PrivilegedEvent.class)
        .map(event -> policyEventConverter.createEvent(event, getOriginalEvent(event)));
  }

  private String policyVarsInternalParameterName() {
    return String.format(POLICY_VARS, policy.getPolicyId());
  }

  private PrivilegedEvent getOriginalEvent(CoreEvent event) {
    return ((InternalEvent) event).getInternalParameter(POLICY_SOURCE_ORIGINAL_EVENT);
  }

}
