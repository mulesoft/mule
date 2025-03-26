/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.mule.runtime.core.internal.streaming.CursorManager.STREAMING_VERBOSE;
import static org.mule.runtime.core.internal.streaming.CursorUtils.unwrap;

import static java.lang.System.identityHashCode;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.streaming.CursorProvider;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import jakarta.inject.Inject;

import org.slf4j.Logger;

/**
 * Tracks instances of {@link ManagedCursorProvider} through the {@link #track(ManagedCursorProvider, Runnable)} method. This
 * class uses a {@link ReferenceQueue} so that when each of those instances are garbage collected, we can make sure that
 * {@link ManagedCursorProvider#releaseResources()} is invoked.
 * <p>
 * This is useful in cases of long running flows in which cursor providers are open and dereferenced long before the flow ends
 * (e.g: an &lt;until-successful&gt; which reads a file many times or a &lt;foreach&gt; that executes many queries which result
 * are never used outside of the scope).
 * <p>
 * Notice that this <b>DOES NOT</b> replace the cleanup and tracking job that is performed in {@link CursorManager}. That still
 * remains the main cleanup mechanism as we cannot rely on garbage collection for releasing streaming resources. This is only a
 * mitigation for cases like described above.
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
    return track(cursorProvider, null);
  }

  /**
   * Tracks the given {@code cursorProvider}
   *
   * @param cursorProvider a {@link ManagedCursorProvider}
   * @param callOnDispose  callback to be called when the {@link StreamingWeakReference} is disposed.
   * @return a {@link WeakReference} wrapping the {@code cursorProvider}
   */
  public WeakReference<ManagedCursorProvider> track(ManagedCursorProvider cursorProvider, Runnable callOnDispose) {
    return new StreamingWeakReference(cursorProvider, referenceQueue, callOnDispose);
  }

  private void bustGhosts() {
    while (!stopped && !currentThread().isInterrupted()) {
      try {
        StreamingWeakReference ghost = (StreamingWeakReference) referenceQueue.remove(POLL_INTERVAL);
        if (ghost != null) {
          bust(ghost);
        }
      } catch (InterruptedException e) {
        currentThread().interrupt();
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Streaming GC thread was interrupted. Finalizing.");
        }
      }
    }
  }

  private void bust(StreamingWeakReference ghost) {
    try {
      if (STREAMING_VERBOSE) {
        CursorProvider innerDelegate = unwrap(ghost.janitor.provider);
        Optional<ComponentLocation> originatingLocation = ghost.janitor.provider.getOriginatingLocation();
        LOGGER.info("StreamingGhostBuster disposing ghost: {}, provider: {} created by {}", ghost.id,
                    identityHashCode(innerDelegate), originatingLocation.map(ComponentLocation::getLocation).orElse("unknown"));
      }
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
   * which the PhantomReference is not used and the ReferenceQueue is applied to the WeakReference directly. This results in a big
   * performance improvement. 2. Because WeakReference#get is not guaranteed to actually return the instance, we need a way to
   * cleanup the resources without actually depending on the ManagedCursorProvider instance. That is when the
   * CursorProviderJanitor comes very handy.
   */
  private class StreamingWeakReference extends WeakReference<ManagedCursorProvider> {

    private final int id;
    private final CursorProviderJanitor janitor;
    private boolean clear = false;
    private final Runnable callOnDispose;

    public StreamingWeakReference(ManagedCursorProvider referent, ReferenceQueue<ManagedCursorProvider> referenceQueue,
                                  Runnable callOnDispose) {
      super(referent, referenceQueue);
      this.janitor = referent.getJanitor();
      this.id = referent.getId();
      this.callOnDispose = callOnDispose;
    }

    public void dispose() {
      if (!clear) {
        clear = true;
        janitor.releaseResources();
        if (callOnDispose != null) {
          callOnDispose.run();
        }
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
