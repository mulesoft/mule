/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.exception.MessagingException;

import org.junit.Test;

public class ExceptionTestCase extends AbstractELTestCase {

  private Error mockError = mock(Error.class);

  public ExceptionTestCase(Variant variant, String mvelOptimizer) {
    super(variant, mvelOptimizer);
  }

  @Override
  public void setupFlowConstruct() throws Exception {
    flowConstruct = getTestFlow();
  }

  @Test
  public void exception() throws Exception {
    MuleEvent event = createEvent();
    RuntimeException rte = new RuntimeException();
    when(mockError.getException()).thenReturn(rte);
    event.setMessage(MuleMessage.builder(event.getMessage()).build());
    assertEquals(rte, evaluate("exception", event));
  }

  @Test
  public void assignException() throws Exception {
    MuleEvent event = createEvent();
    event.setMessage(MuleMessage.builder(event.getMessage()).build());
    RuntimeException runtimeException = new RuntimeException();
    when(mockError.getException()).thenReturn(runtimeException);
    assertImmutableVariable("exception='other'", event);
  }

  @Test
  public void exceptionCausedBy() throws Exception {
    MuleEvent event = createEvent();
    MuleMessage message = event.getMessage();
    MessagingException me =
        new MessagingException(CoreMessages.createStaticMessage(""),
                               MuleEvent.builder(context).message(message).exchangePattern(ONE_WAY).flow(flowConstruct).build(),
                               new IllegalAccessException());
    when(mockError.getException()).thenReturn(me);
    assertTrue((Boolean) evaluate("exception.causedBy(java.lang.IllegalAccessException)", event));
  }

  private MuleEvent createEvent() throws Exception {
    return MuleEvent.builder(DefaultMessageContext.create(flowConstruct, TEST_CONNECTOR))
        .message(MuleMessage.builder().payload("").build()).flow(flowConstruct).error(mockError).build();
  }
}
