/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.context.notification.DefaultFlowCallStack;

import java.util.Optional;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class PayloadTestCase extends AbstractELTestCase {

  private MuleEvent event;
  private MuleMessage message;
  private MuleEvent.Builder eventBuilder;

  public PayloadTestCase(String mvelOptimizer) {
    super(mvelOptimizer);
  }

  @Before
  public void setup() {
    event = mock(MuleEvent.class);
    eventBuilder = mock(MuleEvent.Builder.class);
    when(event.getFlowCallStack()).thenReturn(new DefaultFlowCallStack());
    when(event.getError()).thenReturn(empty());
    message = mock(MuleMessage.class);
    when(event.getMessage()).thenAnswer(invocation -> message);
  }

  @Test
  public void payload() throws Exception {
    Object payload = new Object();
    when(message.getPayload()).thenReturn(payload);
    assertSame(payload, evaluate("payload", event));
  }

  @Test
  public void assignPayload() throws Exception {
    message = MuleMessage.builder().payload("").build();
    when(event.getMessage()).thenReturn(message);
    evaluate("payload = 'foo'", event, eventBuilder);
    ArgumentCaptor<MuleMessage> argument = ArgumentCaptor.forClass(MuleMessage.class);
    verify(eventBuilder).message(argument.capture());
    assertThat(argument.getValue().getPayload(), equalTo("foo"));
  }

}
