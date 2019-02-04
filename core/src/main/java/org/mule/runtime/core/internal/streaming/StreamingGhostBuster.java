/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;


import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import javax.inject.Inject;

import org.slf4j.Logger;

public class StreamingGhostBuster implements Lifecycle {

  private static final long POLL_INTERVAL = SECONDS.toMillis(5);
  private static final Logger LOGGER = getLogger(StreamingGhostBuster.class);

  private final ReferenceQueue<ManagedCursorProvider> referenceQueue = new ReferenceQueue<>();
  private volatile boolean stopped = false;
  private Future taskHandle;

  @Inject
  private SchedulerService schedulerService;

  private Scheduler scheduler;

  @Override
  public void initialise() throws InitialisationException {
    scheduler = schedulerService.customScheduler(SchedulerConfig.config()
        .withMaxConcurrentTasks(1)
        .withName("StreamingManager-CursorProviderCollector"));
  }

  @Override
  public void start() throws MuleException {
    try {
      taskHandle = scheduler.submit(this::ghostBust);
    } catch (RejectedExecutionException e) {
      throw new MuleRuntimeException(e);
    }
    stopped = false;
  }

  @Override
  public void stop() throws MuleException {
    stopped = true;
    taskHandle.cancel(true);
    taskHandle = null;
  }

  @Override
  public void dispose() {
    scheduler.stop();
  }

  public StreamingWeakReference track(ManagedCursorProvider cursorProvider) {
    return new StreamingWeakReference(cursorProvider, referenceQueue);
  }

  private void ghostBust() {
    while (!stopped && !currentThread().isInterrupted()) {
      try {
        StreamingWeakReference ghost = (StreamingWeakReference) referenceQueue.remove(POLL_INTERVAL);
        if (ghost != null) {
          bust(ghost);
        }
      } catch (InterruptedException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Streaming GC thread was interrupted. Finalizing.");
        }
      }
    }
  }

  private void bust(StreamingWeakReference ghost) {
    try {
      ghost.dispose();
    } catch (Exception e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("Found exception trying to dispose phantom CursorProvider: " + e.getMessage(), e);
      }
    } finally {
      ghost.clear();
    }
  }

  private class StreamingWeakReference extends WeakReference<ManagedCursorProvider> {

    private final CursorProviderJanitor janitor;
    private boolean clear = false;

    public StreamingWeakReference(ManagedCursorProvider referent, ReferenceQueue<ManagedCursorProvider> referenceQueue) {
      super(referent, referenceQueue);
      this.janitor = referent.getJanitor();
    }

    public void dispose() {
      if (!clear) {
        clear = true;
        janitor.releaseResources();
      }
    }

    @Override
    public ManagedCursorProvider get() {
      return clear ? null : super.get();
    }

    @Override
    public void clear() {
      super.clear();
      clear = true;
    }
  }
}
