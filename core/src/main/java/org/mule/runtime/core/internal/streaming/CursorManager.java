/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.mule.runtime.core.internal.streaming.CursorManager.Status.DISPOSABLE;
import static org.mule.runtime.core.internal.streaming.CursorManager.Status.NORMAL;
import static org.mule.runtime.core.internal.streaming.CursorManager.Status.SURVIVOR;
import static reactor.core.publisher.Mono.from;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private MutableStreamingStatistics statistics;

  /**
   * Creates a new instance
   *
   * @param statistics statistics which values should be kept updated
   */
  public CursorManager(MutableStreamingStatistics statistics) {
    this.statistics = statistics;
  }

  /**
   * Becomes aware of the given {@code provider} being created by the given {@code creatorEvent}
   *
   * @param provider     the provider to be tracked
   * @param creatorEvent the event that created the provider
   * @return a {@link CursorProviderHandle}
   */
  public CursorProviderHandle track(CursorProvider provider, Event creatorEvent) {
    final EventContext ownerContext = getRoot(creatorEvent.getContext());
    registerEventContext(ownerContext);
    registry.getUnchecked(ownerContext.getId()).addProvider(provider);
    statistics.incrementOpenProviders();

    return new CursorProviderHandle(provider, ownerContext);
  }

  /**
   * Acknowledges that the given {@code cursor} has been opened
   *
   * @param cursor         the opnened cursor
   * @param providerHandle the handle for the provider that generated it
   */
  public void onOpen(Cursor cursor, CursorProviderHandle providerHandle) {
    registry.getUnchecked(providerHandle.getOwnerContext().getId()).addCursor(providerHandle.getCursorProvider(), cursor);
    statistics.incrementOpenCursors();
  }


  /**
   * Acknowledges that the given {@code cursor} has been closed
   *
   * @param cursor         the closed cursor
   * @param handle the handle for the provider that generated it
   */
  public void onClose(Cursor cursor, CursorProviderHandle handle) {
    final String eventId = handle.getOwnerContext().getId();
    EventStreamingState state = registry.getIfPresent(eventId);

    if (state != null && state.removeCursor(handle.getCursorProvider(), cursor) == DISPOSABLE) {
      state.dispose();
      registry.invalidate(eventId);
    }
  }

  private void terminated(EventContext rootContext) {
    if (!rootContext.isStreaming()) {
      return;
    }

    EventStreamingState state = registry.getIfPresent(rootContext.getId());
    if (state != null) {
      if (state.terminate() == DISPOSABLE) {
        state.dispose();
        registry.invalidate(rootContext.getId());
      }
    }
  }

  /**
   * Duplicate registration will occur if cursors are opened in multiple child flows or processing branches. This means terminate
   * will fire multiple times sequentially during completion of the parent EventContext. After the first terminate all other
   * invocation will literally be no-ops. This is preferred to introducing contention here given multiple thread may be opening
   * cursors concurrently.
   */
  private void registerEventContext(EventContext eventContext) {
    from(eventContext.getCompletionPublisher()).doFinally(signal -> terminated(eventContext)).subscribe();
  }

  private EventContext getRoot(EventContext eventContext) {
    return eventContext.getParentContext()
        .map(this::getRoot)
        .orElse(eventContext);
  }

  private class EventStreamingState {

    private Status status = NORMAL;
    private boolean disposed = false;

    private final LoadingCache<CursorProvider, List<Cursor>> cursors = CacheBuilder.newBuilder()
        .build(new CacheLoader<CursorProvider, List<Cursor>>() {

          @Override
          public List<Cursor> load(CursorProvider key) throws Exception {
            return new LinkedList<>();
          }
        });

    private synchronized void addProvider(CursorProvider adapter) {
      cursors.getUnchecked(adapter);
    }

    private Status terminate() {
      if (cursors.size() == 0) {
        status = DISPOSABLE;
      } else {
        boolean allCursorsCanBeReleased = true;
        for (Map.Entry<CursorProvider, List<Cursor>> entry : cursors.asMap().entrySet()) {
          closeProvider(entry.getKey());
          final List<Cursor> cursors = entry.getValue();
          if (!cursors.isEmpty()) {
            allCursorsCanBeReleased = allCursorsCanBeReleased && cursors.stream().allMatch(Cursor::canBeReleased);
          }
        }
        status = allCursorsCanBeReleased ? DISPOSABLE : SURVIVOR;
      }
      return status;
    }

    private void addCursor(CursorProvider provider, Cursor cursor) {
      cursors.getUnchecked(provider).add(cursor);
    }

    private Status removeCursor(CursorProvider provider, Cursor cursor) {
      List<Cursor> openCursors = cursors.getUnchecked(provider);
      if (openCursors.remove(cursor)) {
        statistics.decrementOpenCursors();
      }

      if (openCursors.isEmpty()) {
        if (provider.isClosed() || status == SURVIVOR) {
          dispose();
          status = DISPOSABLE;
          cursors.invalidate(provider);
        }
      }

      return status;
    }

    private void dispose() {
      if (disposed) {
        return;
      }

      cursors.asMap().forEach((provider, cursors) -> {
        try {
          closeProvider(provider);
          releaseAll(cursors);
        } finally {
          LifecycleUtils.disposeIfNeeded(provider, LOGGER);
        }
      });

      disposed = true;
    }

    private void releaseAll(List<Cursor> cursors) {
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


  enum Status {
    NORMAL, SURVIVOR, DISPOSABLE
  }
}
