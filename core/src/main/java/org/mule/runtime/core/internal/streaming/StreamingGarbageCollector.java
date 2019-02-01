/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;


import static java.lang.Thread.currentThread;
import static java.util.Collections.newSetFromMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.Scheduler;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

class StreamingGarbageCollector {

  private static final long POLL_INTERVAL = SECONDS.toMillis(5);

  private final Scheduler scheduler;
  private final ReferenceQueue<ManagedCursorProvider> referenceQueue = new ReferenceQueue<>();
  private final Set<StreamingPhantom> phantoms = newSetFromMap(new ConcurrentHashMap<>());
  private volatile boolean stopped = false;
  private Future taskHandle;

  public StreamingGarbageCollector(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  public void start() {
    try {
      taskHandle = scheduler.submit(this::collectPhantoms);
    } catch (RejectedExecutionException e) {
      throw new MuleRuntimeException(e);
    }
    stopped = false;
  }

  public void stop() {
    stopped = true;
    taskHandle.cancel(true);
    taskHandle = null;
  }

  //public WeakReference<ManagedCursorProvider> track(ManagedCursorProvider cursorProvider) {
  //  return new WeakReference<>(cursorProvider, referenceQueue);
  //  //return new LocaWeakReference(cursorProvider);
  //}

  public void track(ManagedCursorProvider cursorProvider) {
    phantoms.add(new StreamingPhantom(cursorProvider, referenceQueue));
    //return new LocaWeakReference(cursorProvider);
  }

  private void collectPhantoms() {
    int phantomCollected = 0;
    int collected = 0;
    while (!stopped && !currentThread().isInterrupted()) {
      try {
        StreamingPhantom phantom = (StreamingPhantom) referenceQueue.remove(POLL_INTERVAL);
        if (phantom != null) {
          try {
            phantom.dispose();
            System.out.println("Collected: " + ++collected);
          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            phantom.clear();
            phantoms.remove(phantom);
          }
        } else {
          System.out.println("phantom collects: " + ++phantomCollected);
        }
      } catch (InterruptedException e) {
        // log
        break;
      }
    }

    phantoms.clear();
  }

  private class LocaWeakReference extends WeakReference<ManagedCursorProvider> {

    public LocaWeakReference(ManagedCursorProvider referent) {
      super(referent);
    }

    @Override
    public boolean enqueue() {
      ManagedCursorProvider provider = get();
      if (provider != null) {
        provider.releaseResources();
      }
      return super.enqueue();
    }
  }


  private class StreamingPhantom extends PhantomReference<ManagedCursorProvider> {

    private final CursorProviderJanitor janitor;

    public StreamingPhantom(ManagedCursorProvider referent, ReferenceQueue<ManagedCursorProvider> q) {
      super(referent, q);
      janitor = referent.getJanitor();
    }

    public void dispose() {
      janitor.releaseResources();
    }
  }
}
