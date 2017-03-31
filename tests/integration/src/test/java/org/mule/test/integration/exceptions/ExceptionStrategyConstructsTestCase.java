/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import org.mule.functional.exceptions.FunctionalTestException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.message.ExceptionMessage;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class ExceptionStrategyConstructsTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-strategy-constructs-config-flow.xml";
  }

  @Test
  public void testDefaultExceptionStrategySingleEndpoint() throws Exception {
    MuleClient client = muleContext.getClient();

    flowRunner("testService").withPayload(TEST_PAYLOAD).dispatch();
    assertExceptionMessage(client.request("test://modelout", RECEIVE_TIMEOUT).getRight().get());

    flowRunner("testService1").withPayload(TEST_PAYLOAD).dispatch();
    assertExceptionMessage(client.request("test://service1out", RECEIVE_TIMEOUT).getRight().get());

    flowRunner("testflow1").withPayload(TEST_PAYLOAD).dispatch();
    assertExceptionMessage(client.request("test://flow1out", RECEIVE_TIMEOUT).getRight().get());
  }

  private void assertExceptionMessage(Message out) {
    assertThat(out, notNullValue());
    assertThat(out.getPayload().getValue(), instanceOf(ExceptionMessage.class));
    ExceptionMessage exceptionMessage = (ExceptionMessage) out.getPayload().getValue();
    Class clazz = FunctionalTestException.class;
    assertThat(exceptionMessage.getException(), either(hasCause(instanceOf(clazz))).or(hasCause(hasCause(instanceOf(clazz)))));
    assertThat(exceptionMessage.getPayload(), is("test"));
  }

  public static class ExceptionThrowingProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      throw new MessagingException(event, new FunctionalTestException());
    }
  }
}
