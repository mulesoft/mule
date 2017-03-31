/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.routing.outbound;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.message.ExceptionMessage;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.ByteArrayInputStream;

import org.junit.Test;

public class MulticastRouterTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/outbound/multicasting-router-config.xml";
  }

  @Test
  public void testAll() throws Exception {
    ByteArrayInputStream bis = new ByteArrayInputStream("Hello, world".getBytes("UTF-8"));
    MuleClient client = muleContext.getClient();
    flowRunner("all").withPayload(bis).dispatch();

    Message error = client.request("test://errors", 2000).getRight().get();
    assertRoutingExceptionReceived(error);
  }

  @Test
  public void testFirstSuccessful() throws Exception {
    ByteArrayInputStream bis = new ByteArrayInputStream("Hello, world".getBytes("UTF-8"));

    MuleClient client = muleContext.getClient();
    flowRunner("first-successful").withPayload(bis).dispatch();

    Message error = client.request("test://errors2", 2000).getRight().get();
    assertRoutingExceptionReceived(error);
  }

  /**
   * Asserts that a {@link RoutingException} has been received.
   *
   * @param message The received message.
   */
  private void assertRoutingExceptionReceived(Message message) {
    assertThat(message, is(notNullValue()));
    Object payload = message.getPayload().getValue();
    assertThat(payload, is(notNullValue()));
    assertThat(payload, is(instanceOf(ExceptionMessage.class)));
    ExceptionMessage exceptionMessage = (ExceptionMessage) payload;
    assertThat(exceptionMessage.getException(), is(instanceOf(MessagingException.class)));
    assertThat(exceptionMessage.getException().getCause(), is(instanceOf(RoutingException.class)));
  }
}
