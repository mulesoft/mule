/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static java.lang.System.identityHashCode;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

/**
 * Base class for a {@link CursorProvider} decorator which makes sure that {@link Cursor cursors}
 * opened by the decorated provider are properly tracked through the {@link CursorManager}.
 *
 * @param <T> the generic type of the actual {@link Cursor} types that will be produced
 * @see CursorManager
 * @since 4.0
 */
public abstract class ManagedCursorProvider<T extends Cursor> implements CursorProvider<T> {

  private static final Logger LOGGER = getLogger(ManagedCursorProvider.class);

  private final CursorProvider<T> delegate;
  private final CursorManager cursorManager;
  private final CursorContext cursorContext; //TODO: Seems no longer necessary
  private final Map<Integer, WeakReference<Cursor>> cursors = new ConcurrentHashMap<>();
  private final MutableStreamingStatistics statistics;

  private AtomicBoolean released = new AtomicBoolean(false);

  protected ManagedCursorProvider(CursorContext cursorContext, CursorManager cursorManager,
                                  MutableStreamingStatistics statistics) {
    this.delegate = (CursorProvider<T>) cursorContext.getCursorProvider();
    this.cursorContext = cursorContext;
    this.cursorManager = cursorManager;
    this.statistics = statistics;
  }

  /**
   * Gets a cursor from the {@link #delegate} and registers it through {@link CursorManager#onOpen(Cursor, CursorContext)}.
   * <p>
   * The returned cursor will also be managed through the means of {@link #managedCursor(Cursor, CursorContext)}
   *
   * @return a new {@link Cursor}
   */
  @Override
  public final T openCursor() {
    T cursor = delegate.openCursor();
    cursorManager.onOpen(cursor, cursorContext);
    T managedCursor = managedCursor(cursor, cursorContext);
    cursors.put(identityHashCode(managedCursor), new WeakReference<>(managedCursor));

    statistics.incrementOpenCursors();
    return managedCursor;
  }

  public final void onClose(Cursor cursor) {
    if (cursors.remove(identityHashCode(cursor)) != null) {
      try {
        cursor.release();
      } finally {
        statistics.decrementOpenCursors();
        if (isClosed() && cursors.isEmpty()) {
          releaseResources();
        }
      }
    }
  }

  /**
   * Returns a managed version of the {@code cursor}. How will that cursor be managed depends on each
   * implementation. Although it is possible that the same input {@code cursor} is returned, the assumption
   * should be that a new instance will be returned.
   *
   * @param cursor the cursor to manage
   * @param handle the {@link CursorContext}
   * @return a managed {@link Cursor}
   */
  protected abstract T managedCursor(T cursor, CursorContext handle);

  @Override
  public final void releaseResources() {
    if (!released.compareAndSet(false, true)) {
      return;
    }

    try {
      cursors.forEach((hash, weakReference) -> {
        try {
          Cursor cursor = weakReference.get();
          if (cursor != null) {
            weakReference.clear();
            cursor.release();
            statistics.decrementOpenCursors();
          }
        } catch (Exception e) {
          LOGGER.warn("Exception was found trying to close cursor. Execution will continue", e);
        }
      });
    } finally {
      delegate.releaseResources();
    }
  }

  @Override
  public void close() {
    delegate.close();
  }

  @Override
  public boolean isClosed() {
    return delegate.isClosed();
  }

  //TODO: Seems no longer necessary
  protected CursorManager getCursorManager() {
    return cursorManager;
  }
}
