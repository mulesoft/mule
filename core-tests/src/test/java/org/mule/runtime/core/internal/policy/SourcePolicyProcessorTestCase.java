/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import org.junit.Test;

public class SourcePolicyProcessorTestCase extends AbstractPolicyProcessorTestCase {

  @Override
  protected Processor getProcessor() {
    return new SourcePolicyProcessor(policy, policyStateHandler, flowProcessor);
  }

  @Test
  public void messageModifiedBeforeNextProcessorIsNotPropagatedToItWhenPropagationDisabled() throws MuleException {
    CoreEvent modifiedMessageEvent = CoreEvent.builder(initialEvent).message(MESSAGE).build();
    when(flowProcessor.apply(any())).thenReturn(just(modifiedMessageEvent));
    when(policy.getPolicyChain().isPropagateMessageTransformations()).thenReturn(false);
    when(policy.getPolicyChain().apply(any()))
        .thenAnswer(invocation -> just(modifiedMessageEvent).transform(policyStateHandler.retrieveNextOperation(executionId)));

    just(initialEvent).transform(policyProcessor).block();

    verify(flowProcessor).apply(eventCaptor.capture());
    assertEquals(((CoreEvent) from(eventCaptor.getValue()).block()).getMessage(), initialEvent.getMessage());
  }
}
