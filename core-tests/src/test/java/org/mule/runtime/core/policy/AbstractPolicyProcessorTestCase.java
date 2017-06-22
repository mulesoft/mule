/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.session.DefaultMuleSession;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.message.StringAttributes;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public abstract class AbstractPolicyProcessorTestCase extends AbstractMuleTestCase {

  private static final String INIT_VAR_NAME = "initVarName";
  private static final String INIT_VAR_VALUE = "initVarValue";
  private static final String ADDED_VAR_NAME = "addedVarName";
  private static final String ADDED_VAR_VALUE = "addedVarValue";
  private static final String PAYLOAD = "payload";

  private static final Message MESSAGE = Message.builder().payload(PAYLOAD).attributes(new StringAttributes()).build();

  protected Policy policy = mock(Policy.class, RETURNS_DEEP_STUBS);
  protected Processor flowProcessor = mock(Processor.class);
  protected PolicyStateHandler policyStateHandler;
  private ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
  private FlowConstruct mockFlowConstruct = mock(FlowConstruct.class, RETURNS_DEEP_STUBS);
  private Processor policyProcessor;
  private String executionId;
  private Event initialEvent;

  @Before
  public void before() {
    executionId = randomUUID().toString();
    initialEvent = createTestEvent();

    policyStateHandler = new DefaultPolicyStateHandler();
    policyProcessor = getProcessor();
  }

  protected abstract Processor getProcessor();

  @Test
  public void variablesAddedInNextProcessorNotPropagated() throws MuleException {
    Event initialEventWithVars = Event.builder(initialEvent).addVariable(INIT_VAR_NAME, INIT_VAR_VALUE).build();
    Event modifiedVarsEvent = Event.builder(initialEvent).addVariable(ADDED_VAR_NAME, ADDED_VAR_VALUE).build();
    when(flowProcessor.process(any())).thenReturn(modifiedVarsEvent);
    when(policy.getPolicyChain().process(any()))
        .thenAnswer(invocation -> policyStateHandler.retrieveNextOperation(executionId).process(initialEventWithVars));

    Event resultEvent = policyProcessor.process(initialEventWithVars);

    assertEquals(resultEvent.getVariableNames(), initialEventWithVars.getVariableNames());
  }

  @Test
  public void variablesAddedBeforeNextProcessorNotPropagatedToIt() throws MuleException {
    Event initialEventWithVars = Event.builder(initialEvent).addVariable(INIT_VAR_NAME, INIT_VAR_VALUE).build();
    Event modifiedVarsEvent = Event.builder(initialEvent).addVariable(ADDED_VAR_NAME, ADDED_VAR_VALUE).build();
    when(flowProcessor.process(any())).thenReturn(initialEventWithVars);
    when(policy.getPolicyChain().process(any()))
        .thenAnswer(invocation -> policyStateHandler.retrieveNextOperation(executionId).process(modifiedVarsEvent));

    policyProcessor.process(initialEventWithVars);

    verify(flowProcessor).process(eventCaptor.capture());
    assertEquals(eventCaptor.getValue().getVariableNames(), initialEventWithVars.getVariableNames());
  }

  @Test
  public void messageModifiedByNextProcessorIsPropagated() throws MuleException {
    Event modifiedMessageEvent = Event.builder(initialEvent).message(MESSAGE).build();
    when(flowProcessor.process(any())).thenReturn(modifiedMessageEvent);
    when(policy.getPolicyChain().process(any()))
        .thenAnswer(invocation -> policyStateHandler.retrieveNextOperation(executionId).process(initialEvent));

    Event resultEvent = policyProcessor.process(initialEvent);

    assertEquals(resultEvent.getMessage(), MESSAGE);
  }

  @Test
  public void messageModifiedBeforeNextProcessorIsPropagatedToIt() throws MuleException {
    Event modifiedMessageEvent = Event.builder(initialEvent).message(MESSAGE).build();
    when(flowProcessor.process(any())).thenReturn(modifiedMessageEvent);
    when(policy.getPolicyChain().process(any()))
        .thenAnswer(invocation -> policyStateHandler.retrieveNextOperation(executionId).process(modifiedMessageEvent));

    policyProcessor.process(initialEvent);

    verify(flowProcessor).process(eventCaptor.capture());
    assertEquals(eventCaptor.getValue().getMessage(), MESSAGE);
  }

  @Test
  public void sessionModifiedByNextProcessorIsPropagated() throws MuleException {
    DefaultMuleSession session = new DefaultMuleSession();
    Event modifiedSessionEvent = Event.builder(initialEvent).session(session).build();
    when(flowProcessor.process(any())).thenReturn(modifiedSessionEvent);
    when(policy.getPolicyChain().process(any()))
        .thenAnswer(invocation -> policyStateHandler.retrieveNextOperation(executionId).process(initialEvent));

    Event resultEvent = policyProcessor.process(initialEvent);

    assertEquals(resultEvent.getSession(), session);
  }

  @Test
  public void sessionModifiedBeforeNextProcessorIsPropagatedToIt() throws MuleException {
    DefaultMuleSession session = new DefaultMuleSession();
    Event modifiedSessionEvent = Event.builder(initialEvent).session(session).build();
    when(flowProcessor.process(any())).thenReturn(modifiedSessionEvent);
    when(policy.getPolicyChain().process(any()))
        .thenAnswer(invocation -> policyStateHandler.retrieveNextOperation(executionId).process(modifiedSessionEvent));

    policyProcessor.process(initialEvent);

    verify(flowProcessor).process(eventCaptor.capture());
    assertEquals(eventCaptor.getValue().getSession(), session);
  }

  private Event createTestEvent() {
    when(mockFlowConstruct.getUniqueIdString()).thenReturn(executionId);
    return Event.builder(DefaultEventContext.create(mockFlowConstruct, fromSingleComponent("http")))
        .message(Message.builder().nullPayload().build())
        .build();
  }
}
