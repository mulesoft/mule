/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
