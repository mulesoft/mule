/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.metadata.DefaultTypedValue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class PayloadTestCase extends AbstractELTestCase {

  private Event event;
  private InternalMessage message;
  private Event.Builder eventBuilder;

  public PayloadTestCase(String mvelOptimizer) {
    super(mvelOptimizer);
  }

  @Before
  public void setup() {
    event = mock(Event.class);
    eventBuilder = mock(Event.Builder.class);
    when(event.getFlowCallStack()).thenReturn(new DefaultFlowCallStack());
    when(event.getError()).thenReturn(empty());
    message = mock(InternalMessage.class);
    when(event.getMessage()).thenAnswer(invocation -> message);
  }

  @Test
  public void payload() throws Exception {
    Object payload = new Object();
    when(message.getPayload()).thenReturn(new DefaultTypedValue<>(payload, DataType.OBJECT));
    assertSame(payload, evaluate("payload", event));
  }

  @Test
  public void assignPayload() throws Exception {
    message = InternalMessage.builder().payload("").build();
    when(event.getMessage()).thenReturn(message);
    evaluate("payload = 'foo'", event, eventBuilder);
    ArgumentCaptor<InternalMessage> argument = ArgumentCaptor.forClass(InternalMessage.class);
    verify(eventBuilder).message(argument.capture());
    assertThat(argument.getValue().getPayload().getValue(), equalTo("foo"));
  }

}
