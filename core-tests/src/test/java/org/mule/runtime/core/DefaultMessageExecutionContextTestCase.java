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

import org.mule.runtime.core.api.MessageExecutionContext;
import org.mule.runtime.core.api.MuleContext;
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
public class DefaultMessageExecutionContextTestCase extends AbstractMuleTestCase {

  private static final String CORRELATION_ID = "correlationIdValue";

  @Mock
  private MuleConfiguration muleConfig;
  @Mock
  private MuleContext muleContext;
  @Mock
  private FlowConstruct flow;


  @Before
  public void before() {
    when(muleContext.getConfiguration()).thenReturn(muleConfig);
    when(flow.getMuleContext()).thenReturn(muleContext);
  }

  @Test
  public void noCorrelationIdInContext() {
    final MessageExecutionContext executionContext = DefaultMessageExecutionContext.createContext(flow);

    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).id(CORRELATION_ID).build();
    final DefaultMuleEvent event = new DefaultMuleEvent(executionContext, message, flow);

    assertThat(event.getCorrelationId(), is(CORRELATION_ID));
  }

  @Test
  public void parentNoCorrelationIdInContext() {
    final MessageExecutionContext executionContext = DefaultMessageExecutionContext.createContext(flow);

    final MuleMessage messageParent = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).id(CORRELATION_ID).build();
    final DefaultMuleEvent eventParent = new DefaultMuleEvent(executionContext, messageParent, flow);
    ((DefaultFlowCallStack) eventParent.getFlowCallStack()).push(new FlowStackElement(flow.getName(), "/0"));
    final DefaultMuleEvent event = new DefaultMuleEvent(executionContext, message, flow);
    event.setParent(eventParent);

    assertThat(event.getCorrelationId(), is(CORRELATION_ID + ":" + "/0"));
  }

  @Test
  public void correlationIdInContext() {
    final MessageExecutionContext executionContext = DefaultMessageExecutionContext.createContext(flow, CORRELATION_ID);

    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final DefaultMuleEvent event = new DefaultMuleEvent(executionContext, message, flow);

    assertThat(event.getCorrelationId(), is(CORRELATION_ID));
  }

  @Test
  public void parentCorrelationIdInContext() {
    final MessageExecutionContext executionContext = DefaultMessageExecutionContext.createContext(flow, CORRELATION_ID);

    final MuleMessage messageParent = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final DefaultMuleEvent eventParent = new DefaultMuleEvent(executionContext, messageParent, flow);
    ((DefaultFlowCallStack) eventParent.getFlowCallStack()).push(new FlowStackElement(flow.getName(), "/0"));
    final DefaultMuleEvent event = new DefaultMuleEvent(executionContext, message, flow);
    event.setParent(eventParent);

    assertThat(event.getCorrelationId(), is(CORRELATION_ID + ":" + "/0"));
  }

  @Test
  public void overrideCorrelationIdInContext() {
    final MessageExecutionContext executionContext = DefaultMessageExecutionContext.createContext(flow, "abcdef");

    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final DefaultMuleEvent event = new DefaultMuleEvent(executionContext, message, flow);
    event.setCorrelation(new Correlation(CORRELATION_ID, null, null));

    assertThat(event.getCorrelationId(), is(CORRELATION_ID));
  }

  @Test
  public void parentOverrideCorrelationIdInContext() {
    final MessageExecutionContext executionContext = DefaultMessageExecutionContext.createContext(flow, "abcdef");

    final MuleMessage messageParent = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final DefaultMuleEvent eventParent = new DefaultMuleEvent(executionContext, messageParent, flow);
    ((DefaultFlowCallStack) eventParent.getFlowCallStack()).push(new FlowStackElement(flow.getName(), "/0"));
    final DefaultMuleEvent event = new DefaultMuleEvent(executionContext, message, flow);
    event.setCorrelation(new Correlation(CORRELATION_ID, null, null));
    event.setParent(eventParent);

    assertThat(event.getCorrelationId(), is(CORRELATION_ID));
  }

  @Test
  public void noCorrelationIdInContextSequence() {
    final MessageExecutionContext executionContext = DefaultMessageExecutionContext.createContext(flow);

    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).id(CORRELATION_ID).build();
    final DefaultMuleEvent event = new DefaultMuleEvent(executionContext, message, flow);
    event.setCorrelation(new Correlation(null, null, 6));

    assertThat(event.getCorrelationId(), is(CORRELATION_ID + ":" + "6"));
  }

  @Test
  public void parentNoCorrelationIdInContextSequence() {
    final MessageExecutionContext executionContext = DefaultMessageExecutionContext.createContext(flow);

    final MuleMessage messageParent = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).id(CORRELATION_ID).build();
    final DefaultMuleEvent eventParent = new DefaultMuleEvent(executionContext, messageParent, flow);
    ((DefaultFlowCallStack) eventParent.getFlowCallStack()).push(new FlowStackElement(flow.getName(), "/0"));
    final DefaultMuleEvent event = new DefaultMuleEvent(executionContext, message, flow);
    event.setCorrelation(new Correlation(null, null, 6));
    event.setParent(eventParent);

    assertThat(event.getCorrelationId(), is(CORRELATION_ID + ":" + "/0" + ":" + "6"));
  }

  @Test
  public void correlationIdInContextSequence() {
    final MessageExecutionContext executionContext = DefaultMessageExecutionContext.createContext(flow, CORRELATION_ID);

    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final DefaultMuleEvent event = new DefaultMuleEvent(executionContext, message, flow);
    event.setCorrelation(new Correlation(null, null, 6));

    assertThat(event.getCorrelationId(), is(CORRELATION_ID + ":" + "6"));
  }

  @Test
  public void parentCorrelationIdInContextSequence() {
    final MessageExecutionContext executionContext = DefaultMessageExecutionContext.createContext(flow, CORRELATION_ID);

    final MuleMessage messageParent = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final DefaultMuleEvent eventParent = new DefaultMuleEvent(executionContext, messageParent, flow);
    ((DefaultFlowCallStack) eventParent.getFlowCallStack()).push(new FlowStackElement(flow.getName(), "/0"));
    final DefaultMuleEvent event = new DefaultMuleEvent(executionContext, message, flow);
    event.setCorrelation(new Correlation(null, null, 6));
    event.setParent(eventParent);

    assertThat(event.getCorrelationId(), is(CORRELATION_ID + ":" + "/0" + ":" + "6"));
  }

  @Test
  public void overrideCorrelationIdInContextSequence() {
    final MessageExecutionContext executionContext = DefaultMessageExecutionContext.createContext(flow, "abcdef");

    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final DefaultMuleEvent event = new DefaultMuleEvent(executionContext, message, flow);
    event.setCorrelation(new Correlation(CORRELATION_ID, null, 6));

    assertThat(event.getCorrelationId(), is(CORRELATION_ID));
  }

  @Test
  public void parentOverrideCorrelationIdInContextSequence() {
    final MessageExecutionContext executionContext = DefaultMessageExecutionContext.createContext(flow, "abcdef");

    final MuleMessage messageParent = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
    final DefaultMuleEvent eventParent = new DefaultMuleEvent(executionContext, messageParent, flow);
    ((DefaultFlowCallStack) eventParent.getFlowCallStack()).push(new FlowStackElement(flow.getName(), "/0"));
    final DefaultMuleEvent event = new DefaultMuleEvent(executionContext, message, flow);
    event.setCorrelation(new Correlation(CORRELATION_ID, null, 6));
    event.setParent(eventParent);

    assertThat(event.getCorrelationId(), is(CORRELATION_ID));
  }

}
