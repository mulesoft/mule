/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultMessageContextTestCase extends AbstractMuleTestCase {

  private static final String GENERATED_CORRELATION_ID = "generatedCorrelationIdValue";
  private static final String CUSTOM_CORRELATION_ID = "customCorrelationIdValue";
  private static final String SERVER_ID = "serverId";

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
    when(flow.getUniqueIdString()).thenReturn(GENERATED_CORRELATION_ID);
    when(flow.getServerId()).thenReturn(SERVER_ID);

    executionContext = DefaultEventContext.create(flow, TEST_CONNECTOR_LOCATION);
    executionContextWithCorrelation =
        DefaultEventContext.create(flow, TEST_CONNECTOR_LOCATION, CUSTOM_CORRELATION_ID);
  }

  @Test
  public void noCorrelationIdInContext() {
    final Message message = of(TEST_PAYLOAD);
    final Event event = Event.builder(executionContext).message(message).flow(flow).build();

    assertThat(event.getCorrelationId(), is(GENERATED_CORRELATION_ID));
  }

  @Test
  public void correlationIdInContext() {
    final Message message = of(TEST_PAYLOAD);
    final Event event = Event.builder(executionContextWithCorrelation).message(message).flow(flow).build();

    assertThat(event.getCorrelationId(), is(CUSTOM_CORRELATION_ID));
  }

  @Test
  public void overrideCorrelationIdInContext() {
    final Message message = of(TEST_PAYLOAD);
    final Event event = Event.builder(executionContextWithCorrelation).message(message).flow(flow)
        .groupCorrelation(empty()).build();

    assertThat(event.getCorrelationId(), is(CUSTOM_CORRELATION_ID));
  }

  @Test
  public void overrideCorrelationIdInContextSequence() {
    final Message message = of(TEST_PAYLOAD);
    final Event event =
        Event.builder(executionContextWithCorrelation).message(message).correlationId(CUSTOM_CORRELATION_ID).flow(flow)
            .groupCorrelation(Optional.of(GroupCorrelation.of(6))).build();

    assertThat(event.getCorrelationId(), is(CUSTOM_CORRELATION_ID));
  }

}
