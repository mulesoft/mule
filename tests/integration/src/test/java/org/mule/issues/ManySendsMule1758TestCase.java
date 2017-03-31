/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class ManySendsMule1758TestCase extends AbstractIntegrationTestCase {

  private static int NUM_MESSAGES = 3000;

  @Override
  protected String getConfigFile() {
    return "org/mule/issues/many-sends-mule-1758-test-flow.xml";
  }

  @Test
  public void testSingleSend() throws Exception {
    Message response = flowRunner("mySynchService").withPayload("Marco").run().getMessage();
    assertNotNull("Response is null", response);
    assertEquals("Polo", response.getPayload().getValue());
  }

  @Test
  public void testManySends() throws Exception {
    long then = System.currentTimeMillis();
    for (int i = 0; i < NUM_MESSAGES; ++i) {
      logger.debug("Message " + i);
      Message response = flowRunner("mySynchService").withPayload("Marco").run().getMessage();
      assertNotNull("Response is null", response);
      assertEquals("Polo", response.getPayload().getValue());
    }
    long now = System.currentTimeMillis();
    logger.info("Total time " + ((now - then) / 1000.0) + "s; per message " + ((now - then) / (1.0 * NUM_MESSAGES)) + "ms");
  }
}
