/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static java.lang.System.identityHashCode;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.internal.streaming.bytes.ManagedCursorStreamProvider;
import org.mule.runtime.core.internal.streaming.object.ManagedCursorIteratorProvider;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FunkyCursorManager extends CursorManager {

  private static Logger LOGGER = LoggerFactory.getLogger(CursorManager.class);

  private final LoadingCache<StreamingSessionKey, EventStreamingState> registry =
      Caffeine.newBuilder()
          .removalListener((key, value, cause) -> ((EventStreamingState) value).dispose())
          .build(key -> {
            registerEventContext(key.getEventContext());
            return new EventStreamingState();
          });

  private final MutableStreamingStatistics statistics;
  private final StreamingGarbageCollector streamingGarbageCollector;

  /**
   * Creates a new instance
   *
   * @param statistics statistics which values should be kept updated
   */
  public FunkyCursorManager(MutableStreamingStatistics statistics, Scheduler scheduler) {
    super(statistics);
    this.statistics = statistics;
    streamingGarbageCollector = new StreamingGarbageCollector(scheduler);
  }

  @Override
  public void start() throws MuleException {
    streamingGarbageCollector.start();
  }

  @Override
  public void stop() throws MuleException {
    streamingGarbageCollector.stop();
  }

  /**
   * Becomes aware of the given {@code provider} and returns a replacement provider which is managed by the runtime, allowing for
   * automatic resource handling
   *
   * @param provider     the provider to be tracked
   * @param ownerContext the root context of the event that created the provider
   * @return a {@link CursorProvider}
   */
  @Override
  public CursorProvider manage(CursorProvider provider, BaseEventContext ownerContext) {
    final CursorContext context = new CursorContext(provider, ownerContext);

    ManagedCursorProvider managedProvider;
    if (provider instanceof CursorStreamProvider) {
      managedProvider = new ManagedCursorStreamProvider(context, this, statistics);
    } else if (provider instanceof CursorIteratorProvider) {
      managedProvider = new ManagedCursorIteratorProvider(context, this, statistics);
    } else {
      throw new MuleRuntimeException(createStaticMessage("Unknown cursor provider type: " + context.getClass().getName()));
    }

    registry.get(new StreamingSessionKey(ownerContext)).addProvider(managedProvider);

    return managedProvider;
  }

  /**
   * Acknowledges that the given {@code cursor} has been opened
   *
   * @param cursor         the opened cursor
   * @param providerHandle the handle for the provider that generated it
   */
  @Override
  public void onOpen(Cursor cursor, CursorContext providerHandle) {
    //registry.get(providerHandle.getOwnerContext().getId()).addCursor(providerHandle.getCursorProvider(), cursor);
    //statistics.incrementOpenCursors();
  }


  /**
   * Acknowledges that the given {@code cursor} has been closed
   *
   * @param cursor the closed cursor
   * @param handle the handle for the provider that generated it
   */
  @Override
  public void onClose(Cursor cursor, CursorContext handle) {
    //final String eventId = handle.getOwnerContext().getId();
    //EventStreamingState state = registry.getIfPresent(eventId);
    //
    //if (state != null && state.removeCursor(handle.getCursorProvider(), cursor)) {
    //  registry.invalidate(eventId);
    //}
  }

  private void terminated(BaseEventContext rootContext) {
    EventStreamingState state = registry.getIfPresent(rootContext.getId());
    if (state != null) {
      registry.invalidate(rootContext.getId());
    }
  }

  /**
   * Duplicate registration will occur if cursors are opened in multiple child flows or processing branches. This means terminate
   * will fire multiple times sequentially during completion of the parent EventContext. After the first terminate all other
   * invocation will literally be no-ops. This is preferred to introducing contention here given multiple thread may be opening
   * cursors concurrently.
   */
  private void registerEventContext(BaseEventContext eventContext) {
    eventContext.onTerminated((response, throwable) -> terminated(eventContext));
  }

  private class EventStreamingState {

    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private final Map<Integer, WeakReference<ManagedCursorProvider>> providers = new ConcurrentHashMap<>();

    //private final AtomicInteger cursorCount = new AtomicInteger(0);

    //private final LoadingCache<CursorProvider<CursorStream>, Set<Cursor>> cursors = Caffeine.newBuilder()
    //    .removalListener((CursorProvider<CursorStream> key, Set<Cursor> value, RemovalCause cause) -> {
    //      try {
    //        closeProvider(key);
    //        releaseAll(value);
    //      } finally {
    //        key.releaseResources();
    //      }
    //    })
    //    .build(key -> {
    //      statistics.incrementOpenProviders();
    //      return newSetFromMap(new ConcurrentHashMap<>());
    //    });

    private void addProvider(ManagedCursorProvider provider) {
      providers.put(identityHashCode(provider), streamingGarbageCollector.track(provider));
    }

    //private void addCursor(CursorProvider provider, Cursor cursor) {
    //  cursors.get(provider).add(cursor);
    //  cursorCount.incrementAndGet();
    //}

    //private boolean removeCursor(CursorProvider provider, Cursor cursor) {
    //  Set<Cursor> openCursors = cursors.get(provider);
    //  if (openCursors.remove(cursor)) {
    //    statistics.decrementOpenCursors();
    //    if (cursorCount.decrementAndGet() <= 0 && provider.isClosed()) {
    //      dispose();
    //      return true;
    //    }
    //  }
    //
    //  return false;
    //}

    private void dispose() {
      if (disposed.compareAndSet(false, true)) {
        providers.values().forEach(weakReference -> {
          ManagedCursorProvider provider = weakReference.get();
          if (provider != null) {
            weakReference.clear();
            provider.releaseResources();
          }
        });
      }
    }
    //
    //private void releaseAll(Collection<Cursor> cursors) {
    //  cursors.forEach(cursor -> {
    //    try {
    //      cursor.release();
    //      statistics.decrementOpenCursors();
    //    } catch (Exception e) {
    //      LOGGER.warn("Exception was found trying to close cursor. Execution will continue", e);
    //    }
    //  });
    //}
    //
    //private void closeProvider(CursorProvider provider) {
    //  if (!provider.isClosed()) {
    //    provider.close();
    //    statistics.decrementOpenProviders();
    //  }
    //}
  }

  private class StreamingSessionKey {

    private final String id;
    private final BaseEventContext eventContext;

    public StreamingSessionKey(BaseEventContext eventContext) {
      id = eventContext.getId();
      this.eventContext = eventContext;
    }

    public String getId() {
      return id;
    }

    public BaseEventContext getEventContext() {
      return eventContext;
    }

    @Override
    public boolean equals(Object obj) {
      return id.equals(((StreamingSessionKey) obj).id);
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }
  }
}
