/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.performance;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Not a real test, used to generate load and verify that there are no memory leaks using a profiler.
 */
@Ignore
public class QueryPerformanceTestCase extends FunctionalTestCase {

  private LoadGenerator loadGenerator = new LoadGenerator();

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"integration/derby-datasource.xml", "integration/select/select-default-config.xml"};
  }

  @Override
  public int getTestTimeoutSecs() {
    return 5 * 60;
  }

  @Test
  public void testRequestResponsePerformance() throws Exception {
    loadGenerator.generateLoad(new RequestResponseLoadTask());
    takeANap();
  }

  @Test
  public void testOneWayPerformance() throws Exception {
    Thread outputCleaner = new Thread(new LoadCleaner());
    outputCleaner.start();
    loadGenerator.generateLoad(new OneWayLoadTask());
    takeANap();
    outputCleaner.interrupt();
  }

  private void takeANap() throws InterruptedException {
    Thread.sleep(2 * 60 * 1000);
  }

  private static class LoadCleaner implements Runnable {

    @Override
    public void run() {
      MuleClient client = muleContext.getClient();
      while (!Thread.currentThread().isInterrupted()) {
        try {
          client.request("test://testOut", RECEIVE_TIMEOUT);
        } catch (Exception e) {
          // Ignore
        }
      }
    }
  }

  private class RequestResponseLoadTask implements LoadTask {

    @Override
    public void execute(int messageId) throws Exception {
      logger.info("Thread: " + Thread.currentThread().getName() + " message: " + messageId);
      flowRunner("defaultQueryRequestResponse").withPayload(TEST_MESSAGE).run();
    }
  }

  private class OneWayLoadTask implements LoadTask {

    @Override
    public void execute(int messageId) throws Exception {
      logger.info("Thread: " + Thread.currentThread().getName() + " message: " + messageId);
      flowRunner("defaultQueryOneWay").withPayload(TEST_MESSAGE).asynchronously().run();
    }
  }

}
