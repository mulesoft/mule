/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.test.AbstractIntegrationTestCase;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.component.ComponentException;
import org.mule.runtime.core.exception.AbstractMessagingExceptionStrategy;

import org.junit.Test;

public class ExceptionStrategyReturnMessageTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-strategy-return-message.xml";
  }

  @Test
  public void testReturnPayloadDefaultStrategy() throws Exception {
    try {
      flowRunner("InputService2").withPayload("Test Message").run();
    } catch (ComponentException e) {
      assertThat(e.getEvent().getMessage().getPayload(), is(nullValue()));
    }
  }

  @Test
  public void testReturnPayloadCustomStrategy() throws Exception {
    MuleMessage msg = flowRunner("InputService").withPayload(getTestMuleMessage("Test Message")).run().getMessage();

    assertNotNull(msg);
    assertNotNull(msg.getExceptionPayload());
    assertEquals("Functional Test Service Exception", msg.getExceptionPayload().getMessage());

    assertNotNull(msg.getPayload());
    assertEquals("Ka-boom!", msg.getPayload());
  }

  public static class TestExceptionStrategy extends AbstractMessagingExceptionStrategy {

    @Override
    public MuleEvent handleException(Exception exception, MuleEvent event) {
      MuleEvent result = super.handleException(exception, event);
      event.setMessage(MuleMessage.builder(event.getMessage()).payload("Ka-boom!").build());
      if (exception instanceof MessagingException) {
        ((MessagingException) exception).setHandled(true);
      }

      return result;
    }
  }

}
