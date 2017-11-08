/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static java.util.Collections.newSetFromMap;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.streaming.bytes.ManagedCursorStreamProvider;
import org.mule.runtime.core.internal.streaming.object.ManagedCursorIteratorProvider;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keeps track of active {@link Cursor cursors} and their {@link CursorProvider providers}
 *
 * @since 4.0
 */
public class CursorManager {

  private static Logger LOGGER = LoggerFactory.getLogger(CursorManager.class);

  private final LoadingCache<String, EventStreamingState> registry =
      CacheBuilder.newBuilder()
          .removalListener((RemovalNotification<String, EventStreamingState> notification) -> notification.getValue().dispose())
          .build(new CacheLoader<String, EventStreamingState>() {

            @Override
            public EventStreamingState load(String key) throws Exception {
              return new EventStreamingState();
            }
          });

  private final MutableStreamingStatistics statistics;
  private final Scheduler disposalScheduler;

  /**
   * Creates a new instance
   *
   * @param statistics statistics which values should be kept updated
   * @param disposalScheduler where to run the disposal
   */
  public CursorManager(MutableStreamingStatistics statistics, Scheduler disposalScheduler) {
    this.statistics = statistics;
    this.disposalScheduler = disposalScheduler;
  }

  /**
   * Becomes aware of the given {@code provider} and returns a replacement provider which is managed by the runtime, allowing for
   * automatic resource handling
   *
   * @param provider the provider to be tracked
   * @param creatorEvent the event that created the provider
   * @return a {@link CursorContext}
   */
  public CursorProvider manage(CursorProvider provider, CoreEvent creatorEvent) {
    final BaseEventContext ownerContext = ((BaseEventContext) creatorEvent.getContext()).getRootContext();
    registerEventContext(ownerContext);
    registry.getUnchecked(ownerContext.getId()).addProvider(provider);

    final CursorContext context = new CursorContext(provider, ownerContext);
    if (provider instanceof CursorStreamProvider) {
      return new ManagedCursorStreamProvider(context, this);
    } else if (provider instanceof CursorIteratorProvider) {
      return new ManagedCursorIteratorProvider(context, this);
    }

    throw new MuleRuntimeException(createStaticMessage("Unknown cursor provider type: " + context.getClass().getName()));
  }

  /**
   * Acknowledges that the given {@code cursor} has been opened
   *
   * @param cursor the opnened cursor
   * @param providerHandle the handle for the provider that generated it
   */
  public void onOpen(Cursor cursor, CursorContext providerHandle) {
    registry.getUnchecked(providerHandle.getOwnerContext().getId()).addCursor(providerHandle.getCursorProvider(), cursor);
    statistics.incrementOpenCursors();
  }


  /**
   * Acknowledges that the given {@code cursor} has been closed
   *
   * @param cursor the closed cursor
   * @param handle the handle for the provider that generated it
   */
  public void onClose(Cursor cursor, CursorContext handle) {
    final String eventId = handle.getOwnerContext().getId();
    EventStreamingState state = registry.getIfPresent(eventId);

    if (state != null && state.removeCursor(handle.getCursorProvider(), cursor)) {
      registry.invalidate(eventId);
    }
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

    private AtomicBoolean disposed = new AtomicBoolean(false);
    private AtomicInteger cursorCount = new AtomicInteger(0);

    private final LoadingCache<CursorProvider, Set<Cursor>> cursors = CacheBuilder.newBuilder()
        .removalListener((RemovalListener<CursorProvider, Set<Cursor>>) notification -> {
          try {
            closeProvider(notification.getKey());
            releaseAll(notification.getValue());
          } finally {
            notification.getKey().releaseResources();
          }
        })
        .build(new CacheLoader<CursorProvider, Set<Cursor>>() {

          @Override
          public Set<Cursor> load(CursorProvider key) throws Exception {
            statistics.incrementOpenProviders();
            return newSetFromMap(new ConcurrentHashMap<>());
          }
        });

    private synchronized void addProvider(CursorProvider adapter) {
      cursors.getUnchecked(adapter);
    }

    private void addCursor(CursorProvider provider, Cursor cursor) {
      cursors.getUnchecked(provider).add(cursor);
      cursorCount.incrementAndGet();
    }

    private boolean removeCursor(CursorProvider provider, Cursor cursor) {
      Set<Cursor> openCursors = cursors.getUnchecked(provider);
      if (openCursors.remove(cursor)) {
        statistics.decrementOpenCursors();
        if (cursorCount.decrementAndGet() <= 0 && provider.isClosed()) {
          dispose();
          return true;
        }
      }

      return false;
    }

    private void dispose() {
      if (disposed.compareAndSet(false, true)) {
        try {
          disposalScheduler.execute(() -> {
            cursors.invalidateAll();
          });
        } catch (RejectedExecutionException e) {
          // If the Scheduler is busy and can't accept the task, we perform it in the current thread.
          cursors.invalidateAll();
        }
      }
    }

    private void releaseAll(Collection<Cursor> cursors) {
      cursors.forEach(cursor -> {
        try {
          cursor.release();
          statistics.decrementOpenCursors();
        } catch (Exception e) {
          LOGGER.warn("Exception was found trying to close cursor. Execution will continue", e);
        }
      });
    }

    private void closeProvider(CursorProvider provider) {
      if (!provider.isClosed()) {
        provider.close();
        statistics.decrementOpenProviders();
      }
    }
  }
}
