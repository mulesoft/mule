/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor.POLICY_NEXT_OPERATION;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;
import static reactor.core.publisher.Mono.subscriberContext;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyStateHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.event.DefaultMuleSession;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.message.StringAttributes;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Publisher;

public abstract class AbstractPolicyProcessorTestCase extends AbstractMuleTestCase {

  private static final String PAYLOAD = "payload";

  protected static final Message MESSAGE = Message.builder().value(PAYLOAD).attributesValue(new StringAttributes()).build();

  private static final String INIT_VAR_NAME = "initVarName";
  private static final String INIT_VAR_VALUE = "initVarValue";
  private static final String ADDED_VAR_NAME = "addedVarName";
  private static final String ADDED_VAR_VALUE = "addedVarValue";

  private final MuleContext muleContext = mockContextWithServices();
  protected Policy policy = mock(Policy.class, RETURNS_DEEP_STUBS);
  protected Processor flowProcessor = mock(Processor.class);
  protected PolicyStateHandler policyStateHandler;
  protected CoreEvent initialEvent;
  protected String executionId;
  protected Processor policyProcessor;
  protected ArgumentCaptor<Publisher> eventCaptor = ArgumentCaptor.forClass(Publisher.class);
  private final FlowConstruct mockFlowConstruct = mock(FlowConstruct.class, RETURNS_DEEP_STUBS);

  @Before
  public void before() {
    when(mockFlowConstruct.getMuleContext()).thenReturn(muleContext);

    executionId = randomUUID().toString();
    initialEvent = createTestEvent();

    policyStateHandler = new DefaultPolicyStateHandler();
    policyProcessor = getProcessor();
  }

  protected abstract Processor getProcessor();

  @Test
  public void variablesAddedInNextProcessorNotPropagated() throws MuleException {
    CoreEvent initialEventWithVars = CoreEvent.builder(initialEvent).addVariable(INIT_VAR_NAME, INIT_VAR_VALUE).build();
    CoreEvent modifiedVarsEvent = CoreEvent.builder(initialEvent).addVariable(ADDED_VAR_NAME, ADDED_VAR_VALUE).build();
    when(flowProcessor.apply(any())).thenReturn(just(modifiedVarsEvent));
    when(policy.getPolicyChain().apply(any())).thenAnswer(invocation -> just(initialEventWithVars));

    CoreEvent resultEvent = just(initialEventWithVars).transform(policyProcessor).block();

    assertEquals(resultEvent.getVariables().keySet(), initialEventWithVars.getVariables().keySet());
  }

  @Test
  public void variablesAddedBeforeNextProcessorNotPropagatedToIt() throws MuleException {
    CoreEvent initialEventWithVars = CoreEvent.builder(initialEvent).addVariable(INIT_VAR_NAME, INIT_VAR_VALUE).build();
    CoreEvent modifiedVarsEvent = CoreEvent.builder(initialEvent).addVariable(ADDED_VAR_NAME, ADDED_VAR_VALUE).build();
    when(flowProcessor.apply(any())).thenReturn(just(initialEventWithVars));
    when(policy.getPolicyChain().apply(any())).thenAnswer(invocation -> {
      return from(invocation.getArgumentAt(0, Publisher.class))
          .flatMap(e -> subscriberContext().flatMap(ctx -> just(modifiedVarsEvent).transform(ctx.get(POLICY_NEXT_OPERATION))));
    });

    just(initialEventWithVars).transform(policyProcessor).block();

    verify(flowProcessor).apply(eventCaptor.capture());
    assertEquals(((CoreEvent) from(eventCaptor.getValue()).block()).getVariables().keySet(),
                 initialEventWithVars.getVariables().keySet());
  }

  @Test
  public void messageModifiedByNextProcessorIsPropagated() throws MuleException {
    CoreEvent modifiedMessageEvent = CoreEvent.builder(initialEvent).message(MESSAGE).build();
    when(flowProcessor.apply(any())).thenReturn(just(modifiedMessageEvent));
    when(policy.getPolicyChain().apply(any())).thenAnswer(invocation -> just(initialEvent).transform(flowProcessor));

    CoreEvent resultEvent = just(initialEvent).transform(policyProcessor).block();

    assertEquals(resultEvent.getMessage(), MESSAGE);
  }

  @Test
  public void messageModifiedBeforeNextProcessorIsPropagatedToIt() throws MuleException {
    CoreEvent modifiedMessageEvent = CoreEvent.builder(initialEvent).message(MESSAGE).build();
    when(flowProcessor.apply(any())).thenReturn(just(modifiedMessageEvent));
    when(policy.getPolicyChain().isPropagateMessageTransformations()).thenReturn(true);
    when(policy.getPolicyChain().apply(any())).thenAnswer(invocation -> {
      return from(invocation.getArgumentAt(0, Publisher.class))
          .flatMap(e -> subscriberContext()
              .flatMap(ctx -> just(modifiedMessageEvent).transform(ctx.get(POLICY_NEXT_OPERATION))));
    });

    just(initialEvent).transform(policyProcessor).block();

    verify(flowProcessor).apply(eventCaptor.capture());
    assertEquals(((CoreEvent) from(eventCaptor.getValue()).block()).getMessage(), MESSAGE);
  }

  @Test
  public void sessionModifiedByNextProcessorIsPropagated() throws MuleException {
    DefaultMuleSession session = new DefaultMuleSession();
    CoreEvent modifiedSessionEvent = PrivilegedEvent.builder(initialEvent).session(session).build();
    when(flowProcessor.apply(any())).thenReturn(just(modifiedSessionEvent));
    when(policy.getPolicyChain().apply(any())).thenAnswer(invocation -> just(initialEvent).transform(flowProcessor));

    CoreEvent resultEvent = just(initialEvent).transform(policyProcessor).block();

    assertEquals(((PrivilegedEvent) resultEvent).getSession(), session);
  }

  @Test
  public void sessionModifiedBeforeNextProcessorIsPropagatedToIt() throws MuleException {
    DefaultMuleSession session = new DefaultMuleSession();
    CoreEvent modifiedSessionEvent = PrivilegedEvent.builder(initialEvent).session(session).build();
    when(flowProcessor.apply(any())).thenReturn(just(modifiedSessionEvent));
    when(policy.getPolicyChain().apply(any())).thenAnswer(invocation -> {
      return from(invocation.getArgumentAt(0, Publisher.class))
          .flatMap(e -> subscriberContext()
              .flatMap(ctx -> just(modifiedSessionEvent).transform(ctx.get(POLICY_NEXT_OPERATION))));
    });

    just(initialEvent).transform(policyProcessor).block();

    verify(flowProcessor).apply(eventCaptor.capture());
    assertEquals(((PrivilegedEvent) from(eventCaptor.getValue()).block()).getSession(), session);
  }

  private CoreEvent createTestEvent() {
    when(mockFlowConstruct.getUniqueIdString()).thenReturn(executionId);
    return CoreEvent.builder(create(mockFlowConstruct, fromSingleComponent("http")))
        .message(Message.builder().nullValue().build())
        .build();
  }
}
