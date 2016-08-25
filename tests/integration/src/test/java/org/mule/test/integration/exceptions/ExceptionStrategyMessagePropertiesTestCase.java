/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class ExceptionStrategyMessagePropertiesTestCase extends AbstractIntegrationTestCase {

  private final int numMessages = 100;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-strategy-message-properties-flow.xml";
  }

  @Test
  public void testException() throws Exception {
    Thread tester1 = new Tester();
    Thread tester2 = new Tester();
    tester1.start();
    tester2.start();

    MuleClient client = muleContext.getClient();
    MuleMessage msg;
    for (int i = 0; i < numMessages; ++i) {
      msg = client.request("test://out", 5000).getRight().get();
      assertNotNull(msg);
      assertEquals("bar", msg.getOutboundProperty("prop"));
    }
  }

  class Tester extends Thread {

    @Override
    public void run() {
      try {
        for (int i = 0; i < numMessages; ++i) {
          flowRunner("inbound").withPayload(TEST_PAYLOAD).withInboundProperty("foo", "bar").asynchronously().run();
        }
      } catch (Exception e) {
        fail(e.getMessage());
      }
    }
  }
}
