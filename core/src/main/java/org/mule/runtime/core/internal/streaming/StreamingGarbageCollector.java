/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;


import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;
import org.mule.runtime.api.scheduler.Scheduler;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

class StreamingGarbageCollector {

  private static final long POLL_INTERVAL = SECONDS.toMillis(5);

  private final Scheduler scheduler;
  private final ReferenceQueue<ManagedCursorProvider> referenceQueue = new ReferenceQueue<>();

  private volatile boolean stopped = false;
  private Future taskHandle;

  public StreamingGarbageCollector(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  public void start() {
    try {
      taskHandle = scheduler.submit(this::collectPhantoms);
    } catch (RejectedExecutionException e) {

    }
    stopped = false;
  }

  public void stop() {
    stopped = true;
    taskHandle.cancel(true);
    taskHandle = null;
  }

  public WeakReference<ManagedCursorProvider> track(ManagedCursorProvider cursorProvider) {
    return new WeakReference<>(cursorProvider, referenceQueue);
  }

  private void collectPhantoms() {
    while (!stopped && !currentThread().isInterrupted()) {
      try {
        Reference ref = referenceQueue.remove(POLL_INTERVAL);
        if (ref != null) {
          try {
            ManagedCursorProvider provider = (ManagedCursorProvider) ref.get();
            if (provider != null) {
              provider.releaseResources();
            }
          } finally {
            ref.clear();
          }
        }
      } catch (InterruptedException e) {
        // log
        return;
      }
    }
  }
}
