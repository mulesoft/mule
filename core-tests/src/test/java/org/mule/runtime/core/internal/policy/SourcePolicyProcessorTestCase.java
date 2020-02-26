/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor.POLICY_NEXT_OPERATION;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;
import static reactor.core.publisher.Mono.subscriberContext;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import java.lang.ref.Reference;

import org.junit.Test;

import reactor.core.publisher.Mono;

public class SourcePolicyProcessorTestCase extends AbstractPolicyProcessorTestCase {

  @Override
  protected ReactiveProcessor getProcessor() {
    return new SourcePolicyProcessor(policy, flowProcessor);
  }

  @Test
  public void messageModifiedBeforeNextProcessorIsNotPropagatedToItWhenPropagationDisabled() throws MuleException {
    CoreEvent modifiedMessageEvent = CoreEvent.builder(initialEvent).message(MESSAGE).build();
    mockFlowReturningEvent(modifiedMessageEvent);
    when(policy.getPolicyChain().isPropagateMessageTransformations()).thenReturn(false);
    when(policy.getPolicyChain().apply(any())).thenAnswer(invocation -> subscriberContext()
        .flatMap(ctx -> Mono.<CoreEvent>from(invocation.getArgument(0))
            .transform((ReactiveProcessor) ((Reference) ctx.get(POLICY_NEXT_OPERATION)).get())));

    just(initialEvent).transform(policyProcessor).block();

    verify(flowProcessor).apply(eventCaptor.capture());
    assertEquals(((CoreEvent) from(eventCaptor.getValue()).block()).getMessage(), initialEvent.getMessage());
  }
}
