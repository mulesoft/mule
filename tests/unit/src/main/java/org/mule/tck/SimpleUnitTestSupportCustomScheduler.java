/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;

public class SimpleUnitTestSupportCustomScheduler extends SimpleUnitTestSupportScheduler {

  public SimpleUnitTestSupportCustomScheduler(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
    super(corePoolSize, threadFactory, handler);
  }

  @Override
  public void stop() {
    super.stop();
    this.shutdownNow();
  }
}
