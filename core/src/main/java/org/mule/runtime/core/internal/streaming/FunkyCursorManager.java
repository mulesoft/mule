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
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.internal.streaming.bytes.ManagedCursorStreamProvider;
import org.mule.runtime.core.internal.streaming.object.ManagedCursorIteratorProvider;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FunkyCursorManager extends CursorManager{

  private static Logger LOGGER = LoggerFactory.getLogger(CursorManager.class);

  private final LoadingCache<String, EventStreamingState> registry =
      Caffeine.newBuilder()
          .removalListener((key, value, cause) -> ((EventStreamingState) value).dispose())
          .build(key -> new EventStreamingState());

  private final MutableStreamingStatistics statistics;

  /**
   * Creates a new instance
   *
   * @param statistics statistics which values should be kept updated
   */
  public FunkyCursorManager(MutableStreamingStatistics statistics) {
    super(statistics);
    this.statistics = statistics;
  }

  /**
   * Becomes aware of the given {@code provider} and returns a replacement provider which is managed by the runtime, allowing for
   * automatic resource handling
   *
   * @param provider the provider to be tracked
   * @param ownerContext the root context of the event that created the provider
   * @return a {@link CursorProvider}
   */
  public CursorProvider manage(CursorProvider provider, BaseEventContext ownerContext) {
    registerEventContext(ownerContext);
    registry.get(ownerContext.getId()).addProvider(provider);

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
    registry.get(providerHandle.getOwnerContext().getId()).addCursor(providerHandle.getCursorProvider(), cursor);
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

    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private final List<WeakReference<CursorProvider>> providers = new LinkedList<>();
    private final AtomicInteger cursorCount = new AtomicInteger(0);

    private final LoadingCache<CursorProvider<CursorStream>, Set<Cursor>> cursors = Caffeine.newBuilder()
        .removalListener((CursorProvider<CursorStream> key, Set<Cursor> value, RemovalCause cause) -> {
          try {
            closeProvider(key);
            releaseAll(value);
          } finally {
            key.releaseResources();
          }
        })
        .build(key -> {
          statistics.incrementOpenProviders();
          return newSetFromMap(new ConcurrentHashMap<>());
        });

    private synchronized void addProvider(CursorProvider provider) {
      //cursors.get(adapter);
      synchronized (providers) { //TODO: ReadWrite lock? Vector?
        providers.add(new WeakReference<>(provider));
      }
    }


    private void addCursor(CursorProvider provider, Cursor cursor) {
      cursors.get(provider).add(cursor);
      cursorCount.incrementAndGet();
    }

    private boolean removeCursor(CursorProvider provider, Cursor cursor) {
      Set<Cursor> openCursors = cursors.get(provider);
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
        cursors.invalidateAll();
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
