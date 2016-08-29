/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MessageContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.message.Correlation;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultMessageContextTestCase extends AbstractMuleTestCase {

  private static final String CORRELATION_ID = "correlationIdValue";
  private static final String MSG_EXEC_CTX_ID = "execCtxIdValue";

  @Mock
  private MuleConfiguration muleConfig;
  @Mock
  private MuleContext muleContext;
  @Mock
  private FlowConstruct flow;

  private MessageContext executionContext;
  private MessageContext executionContextWithCorrelation;

  @Before
  public void before() {
    when(muleContext.getConfiguration()).thenReturn(muleConfig);
    when(muleContext.getId()).thenReturn(CORRELATION_ID);
    when(muleContext.getUniqueIdString()).thenReturn(MSG_EXEC_CTX_ID);
    when(flow.getMuleContext()).thenReturn(muleContext);

    executionContext = DefaultMessageContext.create(flow, TEST_CONNECTOR);
    executionContextWithCorrelation = DefaultMessageContext.create(flow, TEST_CONNECTOR, CORRELATION_ID);
  }

  @Test
  public void noCorrelationIdInContext() {
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleEvent event = MuleEvent.builder(executionContext).message(message).flow(flow).build();

    assertThat(event.getCorrelationId(), is(MSG_EXEC_CTX_ID));
  }

  @Test
  public void parentNoCorrelationIdInContext() {
    final MuleMessage messageParent = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleEvent eventParent = MuleEvent.builder(executionContext).message(messageParent).flow(flow).build();
    ((DefaultFlowCallStack) eventParent.getFlowCallStack()).push(new FlowStackElement(flow.getName(), "/0"));
    final DefaultMuleEvent event = (DefaultMuleEvent) MuleEvent.builder(executionContext).message(message).flow(flow).build();
    event.setParent(eventParent);

    assertThat(event.getCorrelationId(), is(MSG_EXEC_CTX_ID + ":" + "/0"));
  }

  @Test
  public void correlationIdInContext() {
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleEvent event = MuleEvent.builder(executionContextWithCorrelation).message(message).flow(flow).build();

    assertThat(event.getCorrelationId(), is(CORRELATION_ID));
  }

  @Test
  public void parentCorrelationIdInContext() {
    final MuleMessage messageParent = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleEvent eventParent = MuleEvent.builder(executionContextWithCorrelation).message(messageParent).flow(flow).build();
    ((DefaultFlowCallStack) eventParent.getFlowCallStack()).push(new FlowStackElement(flow.getName(), "/0"));
    final DefaultMuleEvent event =
        (DefaultMuleEvent) MuleEvent.builder(executionContextWithCorrelation).message(message).flow(flow).build();
    event.setParent(eventParent);

    assertThat(event.getCorrelationId(), is(CORRELATION_ID + ":" + "/0"));
  }

  @Test
  public void overrideCorrelationIdInContext() {
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final DefaultMuleEvent event =
        (DefaultMuleEvent) MuleEvent.builder(executionContextWithCorrelation).message(message).flow(flow).build();
    event.setCorrelation(new Correlation(null, null));

    assertThat(event.getCorrelationId(), is(CORRELATION_ID));
  }

  @Test
  public void parentOverrideCorrelationIdInContext() {
    final MuleMessage messageParent = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleEvent eventParent = MuleEvent.builder(executionContextWithCorrelation).message(messageParent).flow(flow).build();
    ((DefaultFlowCallStack) eventParent.getFlowCallStack()).push(new FlowStackElement(flow.getName(), "/0"));
    final DefaultMuleEvent event =
        (DefaultMuleEvent) MuleEvent.builder(executionContextWithCorrelation).message(message).flow(flow).build();
    event.setLegacyCorrelationId(CORRELATION_ID);
    event.setParent(eventParent);

    assertThat(event.getCorrelationId(), is(CORRELATION_ID));
  }

  @Test
  public void noCorrelationIdInContextSequence() {
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final DefaultMuleEvent event = (DefaultMuleEvent) MuleEvent.builder(executionContext).message(message).flow(flow).build();
    event.setCorrelation(new Correlation(null, 6));

    assertThat(event.getCorrelationId(), is(MSG_EXEC_CTX_ID + ":" + "6"));
  }

  @Test
  public void parentNoCorrelationIdInContextSequence() {
    final MuleMessage messageParent = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleEvent eventParent = MuleEvent.builder(executionContext).message(messageParent).flow(flow).build();
    ((DefaultFlowCallStack) eventParent.getFlowCallStack()).push(new FlowStackElement(flow.getName(), "/0"));
    final DefaultMuleEvent event = (DefaultMuleEvent) MuleEvent.builder(executionContext).message(message).flow(flow).build();
    event.setCorrelation(new Correlation(null, 6));
    event.setParent(eventParent);

    assertThat(event.getCorrelationId(), is(MSG_EXEC_CTX_ID + ":" + "/0" + ":" + "6"));
  }

  @Test
  public void correlationIdInContextSequence() {
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final DefaultMuleEvent event =
        (DefaultMuleEvent) MuleEvent.builder(executionContextWithCorrelation).message(message).flow(flow).build();
    event.setCorrelation(new Correlation(null, 6));

    assertThat(event.getCorrelationId(), is(CORRELATION_ID + ":" + "6"));
  }

  @Test
  public void parentCorrelationIdInContextSequence() {
    final MuleMessage messageParent = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleEvent eventParent = MuleEvent.builder(executionContextWithCorrelation).message(messageParent).flow(flow).build();
    ((DefaultFlowCallStack) eventParent.getFlowCallStack()).push(new FlowStackElement(flow.getName(), "/0"));
    final DefaultMuleEvent event =
        (DefaultMuleEvent) MuleEvent.builder(executionContextWithCorrelation).message(message).flow(flow).build();
    event.setCorrelation(new Correlation(null, 6));
    event.setParent(eventParent);

    assertThat(event.getCorrelationId(), is(CORRELATION_ID + ":" + "/0" + ":" + "6"));
  }

  @Test
  public void overrideCorrelationIdInContextSequence() {
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final DefaultMuleEvent event =
        (DefaultMuleEvent) MuleEvent.builder(executionContextWithCorrelation).message(message).flow(flow).build();
    event.setCorrelation(new Correlation(null, 6));
    event.setLegacyCorrelationId(CORRELATION_ID);

    assertThat(event.getCorrelationId(), is(CORRELATION_ID));
  }

  @Test
  public void parentOverrideCorrelationIdInContextSequence() {
    final MuleMessage messageParent = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleEvent eventParent = MuleEvent.builder(executionContextWithCorrelation).message(messageParent).flow(flow).build();
    ((DefaultFlowCallStack) eventParent.getFlowCallStack()).push(new FlowStackElement(flow.getName(), "/0"));
    final DefaultMuleEvent event =
        (DefaultMuleEvent) MuleEvent.builder(executionContextWithCorrelation).message(message).flow(flow).build();
    event.setCorrelation(new Correlation(null, 6));
    event.setLegacyCorrelationId(CORRELATION_ID);
    event.setParent(eventParent);

    assertThat(event.getCorrelationId(), is(CORRELATION_ID));
  }

}
