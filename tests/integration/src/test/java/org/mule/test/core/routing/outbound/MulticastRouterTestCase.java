/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.routing.outbound;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.message.ExceptionMessage;

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
    flowRunner("all").withPayload(bis).asynchronously().run();

    MuleMessage error = client.request("test://errors", 2000).getRight().get();
    assertRoutingExceptionReceived(error);
  }

  @Test
  public void testFirstSuccessful() throws Exception {
    ByteArrayInputStream bis = new ByteArrayInputStream("Hello, world".getBytes("UTF-8"));

    MuleClient client = muleContext.getClient();
    flowRunner("first-successful").withPayload(bis).asynchronously().run();

    MuleMessage error = client.request("test://errors2", 2000).getRight().get();
    assertRoutingExceptionReceived(error);
  }

  /**
   * Asserts that a {@link RoutingException} has been received.
   *
   * @param message The received message.
   */
  private void assertRoutingExceptionReceived(MuleMessage message) {
    assertNotNull(message);
    Object payload = message.getPayload();
    assertNotNull(payload);
    assertTrue(payload instanceof ExceptionMessage);
    ExceptionMessage exceptionMessage = (ExceptionMessage) payload;
    assertTrue(exceptionMessage.getException() instanceof RoutingException);
  }
}
