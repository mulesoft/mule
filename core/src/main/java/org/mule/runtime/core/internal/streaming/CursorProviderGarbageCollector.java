/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;


import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MINUTES;
import org.mule.runtime.api.scheduler.Scheduler;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

public class CursorProviderGarbageCollector {

  private static final long POLL_INTERVAL = MINUTES.toMillis(1);

  private final Scheduler scheduler;
  private final ReferenceQueue<ManagedCursorProvider> referenceQueue = new ReferenceQueue<>();

  private volatile boolean stopped = false;
  private Future taskHandle;

  public CursorProviderGarbageCollector(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  public void start() {
    try {
      taskHandle = scheduler.submit();
    } catch (RejectedExecutionException e) {

    }

    stopped = false;

  }

  public PhantomReference<ManagedCursorProvider> track(ManagedCursorProvider cursorProvider) {
    return new PhantomReference<>(cursorProvider, referenceQueue);
  }

  private void collectPhantoms() {
    while (!stopped && !currentThread().isInterrupted()) {
      try {
        Reference ref = referenceQueue.remove(POLL_INTERVAL);
        if (ref != null) {
          ref.
        }
      } catch (InterruptedException e) {
        // log
        return;
      }
    }
  }
}
