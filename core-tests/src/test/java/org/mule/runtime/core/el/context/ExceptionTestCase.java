/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.DefaultMessageContext.create;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.message.ErrorBuilder.builder;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.message.ErrorBuilder;

import org.junit.Test;

public class ExceptionTestCase extends AbstractELTestCase {

  public ExceptionTestCase(Variant variant, String mvelOptimizer) {
    super(variant, mvelOptimizer);
  }

  @Test
  public void exception() throws Exception {
    MuleEvent event = getTestEvent("");
    RuntimeException rte = new RuntimeException();
    event.setError(ErrorBuilder.builder(rte).build());
    event.setMessage(MuleMessage.builder(event.getMessage()).build());
    assertEquals(rte, evaluate("exception", event));
  }

  @Test
  public void assignException() throws Exception {
    MuleEvent event = getTestEvent("");
    event.setMessage(MuleMessage.builder(event.getMessage()).build());
    event.setError(ErrorBuilder.builder(new RuntimeException()).build());
    assertImmutableVariable("exception='other'", event);
  }

  @Test
  public void exceptionCausedBy() throws Exception {
    MuleEvent event = getTestEvent("");
    MuleMessage message = event.getMessage();
    Flow flow = getTestFlow();
    MessagingException me =
        new MessagingException(CoreMessages.createStaticMessage(""),
                               new DefaultMuleEvent(create(flow, "test"), message, ONE_WAY, flow),
                               new IllegalAccessException());
    event.setError(ErrorBuilder.builder(me).build());
    assertTrue((Boolean) evaluate("exception.causedBy(java.lang.IllegalAccessException)", event));
  }
}
