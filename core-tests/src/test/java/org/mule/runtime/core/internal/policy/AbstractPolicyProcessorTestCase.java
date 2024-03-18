/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor.POLICY_NEXT_OPERATION;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import static java.util.UUID.randomUUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Flux.deferContextual;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.event.InternalEvent;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.event.PrivilegedEvent.Builder;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.message.StringAttributes;

import java.lang.ref.Reference;

import org.reactivestreams.Publisher;

import org.junit.Before;
import org.junit.Test;

import org.mockito.ArgumentCaptor;

import reactor.core.publisher.Mono;

public abstract class AbstractPolicyProcessorTestCase extends AbstractMuleTestCase {

  private static final String PAYLOAD = "payload";

  protected static final Message MESSAGE = Message.builder().value(PAYLOAD).attributesValue(new StringAttributes()).build();

  protected final MuleContext muleContext = mockContextWithServices();
  protected Policy policy = mock(Policy.class, RETURNS_DEEP_STUBS);
  protected Processor flowProcessor = mock(Processor.class);
  protected CoreEvent initialEvent;
  protected String executionId;
  protected ReactiveProcessor policyProcessor;
  protected ArgumentCaptor<Publisher> eventCaptor = ArgumentCaptor.forClass(Publisher.class);
  private final FlowConstruct mockFlowConstruct = mock(FlowConstruct.class, RETURNS_DEEP_STUBS);
  private SourcePolicyContext sourcePolicyContext;
  protected OperationPolicyContext operationPolicyContext;

  @Before
  public void before() {
    when(mockFlowConstruct.getMuleContext()).thenReturn(muleContext);
    when(policy.getPolicyChain().isPropagateMessageTransformations()).thenReturn(true);

    executionId = randomUUID().toString();
    operationPolicyContext = mock(OperationPolicyContext.class, RETURNS_DEEP_STUBS);
    sourcePolicyContext = new SourcePolicyContext(mock(PolicyPointcutParameters.class));
    initialEvent = createTestEvent();
    ((InternalEvent) initialEvent).setSourcePolicyContext(sourcePolicyContext);
    ((InternalEvent) initialEvent).setOperationPolicyContext(operationPolicyContext);

    when(flowProcessor.apply(any())).thenAnswer(invocation -> invocation.getArgument(0));
    policyProcessor = getProcessor();

    final PolicyChain policyChain = policy.getPolicyChain();
    when(policyChain.onChainError(any())).thenReturn(policyChain);
  }

  protected abstract ReactiveProcessor getProcessor();

  @Test
  public void messageModifiedByNextProcessorIsPropagated() {
    CoreEvent modifiedMessageEvent = CoreEvent.builder(initialEvent).message(MESSAGE).build();
    mockFlowReturningEvent(modifiedMessageEvent);
    when(policy.getPolicyChain().apply(any()))
        .thenAnswer(invocation -> deferContextual(ctx -> Mono.<CoreEvent>from(invocation.getArgument(0))
            .transform((ReactiveProcessor) ((Reference) ctx.get(POLICY_NEXT_OPERATION)).get())));

    CoreEvent resultEvent = just(initialEvent).transform(policyProcessor).block();

    assertEquals(resultEvent.getMessage(), MESSAGE);
  }

  @Test
  public void messageModifiedBeforeNextProcessorIsPropagatedToIt() {
    when(policy.getPolicyChain().isPropagateMessageTransformations()).thenReturn(true);
    when(policy.getPolicyChain().apply(any()))
        .thenAnswer(invocation -> deferContextual(ctx -> Mono.<CoreEvent>from(invocation.getArgument(0))
            .map(e -> CoreEvent.builder(e).message(MESSAGE).build())
            .transform((ReactiveProcessor) ((Reference) ctx.get(POLICY_NEXT_OPERATION)).get())));

    just(initialEvent).transform(policyProcessor).block();

    verify(flowProcessor).apply(eventCaptor.capture());
    assertEquals(((CoreEvent) from(eventCaptor.getValue()).block()).getMessage(), MESSAGE);
  }

  protected void mockFlowReturningEvent(CoreEvent event) {
    when(flowProcessor.apply(any())).thenAnswer(inv -> from(inv.getArgument(0))
        .map(e -> {
          final Builder builder = PrivilegedEvent.builder((CoreEvent) e)
              .message(event.getMessage())
              .variables(event.getVariables());

          return builder.build();
        }));
  }

  private CoreEvent createTestEvent() {
    when(mockFlowConstruct.getUniqueIdString()).thenReturn(executionId);
    return CoreEvent.builder(create(mockFlowConstruct, fromSingleComponent("http")))
        .message(Message.builder().nullValue().build())
        .build();
  }
}
