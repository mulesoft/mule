/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static reactor.core.publisher.Mono.from;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.internal.streaming.bytes.ManagedCursorStreamProvider;
import org.mule.runtime.core.internal.streaming.object.ManagedCursorIteratorProvider;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;

import java.util.LinkedList;
import java.util.List;

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
   * Becomes aware of the given {@code provider} and returns a replacement provider which is managed by the runtime, allowing for
   * automatic resource handling
   *
   * @param provider the provider to be tracked
   * @param creatorEvent the event that created the provider
   * @return a {@link CursorContext}
   */
  public CursorProvider manage(CursorProvider provider, Event creatorEvent) {
    final EventContext ownerContext = getRoot(creatorEvent.getInternalContext());
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
      state.dispose();
      registry.invalidate(eventId);
    }
  }

  private void terminated(EventContext rootContext) {
    EventStreamingState state = registry.getIfPresent(rootContext.getId());
    if (state != null) {
      state.dispose();
      registry.invalidate(rootContext.getId());
    }
  }

  /**
   * Duplicate registration will occur if cursors are opened in multiple child flows or processing branches. This means terminate
   * will fire multiple times sequentially during completion of the parent EventContext. After the first terminate all other
   * invocation will literally be no-ops. This is preferred to introducing contention here given multiple thread may be opening
   * cursors concurrently.
   */
  private void registerEventContext(EventContext eventContext) {
    from(eventContext.getCompletionPublisher()).subscribe(null, null, () -> terminated(eventContext));
  }

  private EventContext getRoot(EventContext eventContext) {
    return eventContext.getInternalParentContext()
        .map(this::getRoot)
        .orElse(eventContext);
  }

  private class EventStreamingState {

    private boolean disposed = false;

    private final LoadingCache<CursorProvider, List<Cursor>> cursors = CacheBuilder.newBuilder()
        .build(new CacheLoader<CursorProvider, List<Cursor>>() {

          @Override
          public List<Cursor> load(CursorProvider key) throws Exception {
            statistics.incrementOpenProviders();
            return new LinkedList<>();
          }
        });

    private synchronized void addProvider(CursorProvider adapter) {
      cursors.getUnchecked(adapter);
    }

    private void addCursor(CursorProvider provider, Cursor cursor) {
      cursors.getUnchecked(provider).add(cursor);
    }

    private boolean removeCursor(CursorProvider provider, Cursor cursor) {
      List<Cursor> openCursors = cursors.getUnchecked(provider);
      if (openCursors.remove(cursor)) {
        statistics.decrementOpenCursors();
      }

      if (openCursors.isEmpty()) {
        if (provider.isClosed()) {
          dispose();
          cursors.invalidate(provider);
          return true;
        }
      }

      return false;
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
          provider.releaseResources();
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
}
