/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.internal;

import org.mule.runtime.core.api.event.CoreEvent;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class TestQueue {

  private final String name;
  private final LinkedBlockingQueue<CoreEvent> blockingQueue;

  public TestQueue(String name) {
    this.name = name;
    blockingQueue = new LinkedBlockingQueue<>();
  }

  public String getName() {
    return name;
  }

  public void push(CoreEvent coreEvent) throws InterruptedException {
    blockingQueue.put(coreEvent);
  }

  public CoreEvent pop() {
    return blockingQueue.poll();
  }

  public CoreEvent pop(long timeout, TimeUnit timeUnit) throws InterruptedException {
    return blockingQueue.poll(timeout, timeUnit);
  }

  public int countPendingEvents() {
    return blockingQueue.size();
  }
}
