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

/**
 * Tracks instances of {@link ManagedCursorProvider} through the {@link #track(ManagedCursorProvider)} method.
 * This class uses a {@link ReferenceQueue} so that when each of those instances are garbage collected, we can make sure
 * that {@link ManagedCursorProvider#releaseResources()} is invoked.
 * <p>
 * This is useful in cases of long running flows in which cursor providers are open and dereferenced long before the flow ends
 * (e.g: an &lt;until-successful&gt; which reads a file many times or a &lt;foreach&gt; that executes many queries which result
 * are never used outside of the scope).
 * <p>
 * Notice that this <b>DOES NOT</b> replaces the cleanup and tracking job that is performed in {@link CursorManager}. That
 * still remains the main cleanup mechanism as we cannot rely on garbage collection for releasing streaming resources. This is
 * only a mitigation for cases like described above.
 *
 * @since 4.2.0
 */
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
      taskHandle = scheduler.submit(this::bustGhosts);
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

  /**
   * Tracks the given {@code cursorProvider}
   *
   * @param cursorProvider a {@link ManagedCursorProvider}
   * @return a {@link WeakReference} wrapping the {@code cursorProvider}
   */
  public WeakReference<ManagedCursorProvider> track(ManagedCursorProvider cursorProvider) {
    return new StreamingWeakReference(cursorProvider, referenceQueue);
  }

  private void bustGhosts() {
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
        LOGGER.warn("Found exception trying to dispose phantom CursorProvider", e);
      }
    } finally {
      ghost.clear();
    }
  }

  /*
   * MG says:
   *
   * Important implementation details:
   *
   * 1. An original design included a PhantomReference as part of this class state. It was then changed to its current state in
   * which the PhantomReference is not used and the ReferenceQueue is applied to the WeakReference directly. This results in
   * a big performance improvement.
   * 2. Because WeakReference#get is not guaranteed to actually return the instance, we need a way to cleanup the resources
   * without actually depending on the ManagedCursorProvider instance. That is when the CursorProviderJanitor comes very handy.
   */
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
