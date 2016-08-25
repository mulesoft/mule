/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class ExpiredShutdownTimeoutOneWayTestCase extends AbstractShutdownTimeoutRequestResponseTestCase {

  @Rule
  public SystemProperty contextShutdownTimeout = new SystemProperty("contextShutdownTimeout", "100");

  @Override
  protected String getConfigFile() {
    return "shutdown-timeout-one-way-config.xml";
  }

  @Test
  public void testStaticComponent() throws Exception {
    doShutDownTest("staticComponentFlow");
  }

  @Test
  public void testScriptComponent() throws Exception {
    doShutDownTest("scriptComponentFlow");
  }

  @Test
  public void testExpressionTransformer() throws Exception {
    doShutDownTest("expressionTransformerFlow");
  }

  private void doShutDownTest(final String flowName) throws MuleException, InterruptedException {
    final MuleClient client = muleContext.getClient();
    final boolean[] results = new boolean[] {false};

    Thread t = new Thread() {

      @Override
      public void run() {
        try {
          flowRunner(flowName).withPayload(TEST_MESSAGE).asynchronously().run();
          results[0] = !client.request("test://response", RECEIVE_TIMEOUT).getRight().isPresent();
        } catch (Exception e) {
          e.printStackTrace();
          // Ignore
        }
      }
    };
    t.start();

    // Make sure to give the request enough time to get to the waiting portion of the feed.
    waitLatch.await();

    muleContext.stop();

    t.join();

    assertTrue("Was able to process message ", results[0]);
  }
}
