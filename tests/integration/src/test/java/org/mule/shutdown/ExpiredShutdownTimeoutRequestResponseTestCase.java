/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import static org.junit.Assert.assertTrue;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("See MULE-9200")
public class ExpiredShutdownTimeoutRequestResponseTestCase extends AbstractShutdownTimeoutRequestResponseTestCase {

  @Rule
  public SystemProperty contextShutdownTimeout = new SystemProperty("contextShutdownTimeout", "100");

  @Override
  protected String getConfigFile() {
    return "shutdown-timeout-request-response-config.xml";
  }

  @Test
  public void testStaticComponent() throws Exception {
    doShutDownTest("http://localhost:" + httpPort.getNumber() + "/staticComponent");
  }

  @Test
  public void testScriptComponent() throws Exception {
    doShutDownTest("http://localhost:" + httpPort.getNumber() + "/scriptComponent");
  }

  @Test
  public void testExpressionTransformer() throws Exception {
    doShutDownTest("http://localhost:" + httpPort.getNumber() + "/expressionTransformer");
  }

  private void doShutDownTest(final String url) throws MuleException, InterruptedException {
    final MuleClient client = muleContext.getClient();
    final boolean[] results = new boolean[] {false};

    Thread t = new Thread() {

      @Override
      public void run() {
        try {
          MuleMessage muleMessage = MuleMessage.builder().payload(TEST_MESSAGE).build();
          Error error =
              client.send(url, muleMessage,
                          HttpRequestOptionsBuilder.newOptions().disableStatusCodeValidation().method(POST.name()).build())
                  .getLeft();
          results[0] = error.getException() instanceof DispatchException;
        } catch (Exception e) {
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
