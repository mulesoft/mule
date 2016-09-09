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

import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.message.GroupCorrelation;
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

  private EventContext executionContext;
  private EventContext executionContextWithCorrelation;

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
    final InternalMessage message = InternalMessage.builder().payload(TEST_PAYLOAD).build();
    final Event event = Event.builder(executionContext).message(message).flow(flow).build();

    assertThat(event.getCorrelationId(), is(MSG_EXEC_CTX_ID));
  }

  @Test
  public void correlationIdInContext() {
    final InternalMessage message = InternalMessage.builder().payload(TEST_PAYLOAD).build();
    final Event event = Event.builder(executionContextWithCorrelation).message(message).flow(flow).build();

    assertThat(event.getCorrelationId(), is(CORRELATION_ID));
  }

  @Test
  public void overrideCorrelationIdInContext() {
    final InternalMessage message = InternalMessage.builder().payload(TEST_PAYLOAD).build();
    final Event event = Event.builder(executionContextWithCorrelation).message(message).flow(flow)
        .groupCorrelation(new GroupCorrelation(null, null)).build();

    assertThat(event.getCorrelationId(), is(CORRELATION_ID));
  }

  @Test
  public void overrideCorrelationIdInContextSequence() {
    final InternalMessage message = InternalMessage.builder().payload(TEST_PAYLOAD).build();
    final Event event =
        Event.builder(executionContextWithCorrelation).message(message).correlationId(CORRELATION_ID).flow(flow)
            .groupCorrelation(new GroupCorrelation(null, 6)).build();

    assertThat(event.getCorrelationId(), is(CORRELATION_ID));
  }

}
