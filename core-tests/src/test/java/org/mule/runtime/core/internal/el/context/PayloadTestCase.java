/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.context;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class PayloadTestCase extends AbstractELTestCase {

  private InternalEvent event;
  private Message message;
  private InternalEvent.Builder eventBuilder;

  public PayloadTestCase(String mvelOptimizer) {
    super(mvelOptimizer);
  }

  @Before
  public void setup() {
    event = mock(InternalEvent.class);
    eventBuilder = mock(InternalEvent.Builder.class);
    when(event.getFlowCallStack()).thenReturn(new DefaultFlowCallStack());
    when(event.getError()).thenReturn(empty());
    message = mock(InternalMessage.class);
    when(event.getMessage()).thenAnswer(invocation -> message);
  }

  @Test
  public void payload() throws Exception {
    Object payload = new Object();
    when(message.getPayload()).thenReturn(new TypedValue<>(payload, DataType.OBJECT));
    assertSame(payload, evaluate("payload", event));
  }

  @Test
  public void assignPayload() throws Exception {
    message = of("");
    when(event.getMessage()).thenReturn(message);
    evaluate("payload = 'foo'", event, eventBuilder);
    ArgumentCaptor<InternalMessage> argument = ArgumentCaptor.forClass(InternalMessage.class);
    verify(eventBuilder).message(argument.capture());
    assertThat(argument.getValue().getPayload().getValue(), equalTo("foo"));
  }

}
