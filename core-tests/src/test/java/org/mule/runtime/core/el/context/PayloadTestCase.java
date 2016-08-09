/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PayloadTestCase extends AbstractELTestCase {

  private MuleEvent event;
  private MuleMessage message;

  public PayloadTestCase(Variant variant, String mvelOptimizer) {
    super(variant, mvelOptimizer);
  }

  @Before
  public void setup() {
    event = mock(MuleEvent.class);
    message = mock(MuleMessage.class);
    doAnswer(invocation -> {
      message = (MuleMessage) invocation.getArguments()[0];
      return null;
    }).when(event).setMessage(any(MuleMessage.class));
    when(event.getMessage()).thenAnswer(invocation -> message);
    when(event.getMuleContext()).thenReturn(muleContext);
  }

  @Test
  public void payload() throws Exception {
    Object payload = new Object();
    Mockito.when(message.getPayload()).thenReturn(payload);
    assertSame(payload, evaluate("payload", event));
  }

  @Test
  public void assignPayload() throws Exception {
    message = MuleMessage.builder().payload("").build();
    when(event.getMessage()).thenReturn(message);
    evaluate("payload = 'foo'", event);
    assertEquals("foo", message.getPayload());
  }

}
