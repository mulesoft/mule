/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static java.util.Collections.newSetFromMap;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for a {@link CursorProvider} decorator which makes sure that {@link Cursor cursors}
 * opened by the decorated provider are properly tracked through the {@link CursorManager}.
 *
 * @param <T> the generic type of the actual {@link Cursor} types that will be produced
 * @see CursorManager
 * @since 4.0
 */
public abstract class ManagedCursorProvider<T extends Cursor> implements CursorProvider<T> {

  private final CursorProvider<T> delegate;
  private final Set<Cursor> cursors = newSetFromMap(new ConcurrentHashMap<>());
  private final MutableStreamingStatistics statistics;
  private final CursorProviderJanitor janitor;

  protected ManagedCursorProvider(CursorProvider<T> delegate, MutableStreamingStatistics statistics) {
    this.delegate = delegate;
    this.janitor = new CursorProviderJanitor(delegate, cursors, statistics);
    this.statistics = statistics;
    statistics.incrementOpenProviders();
  }

  /**
   * Gets a cursor from the {@link #delegate} and keeps track of it.
   * <p>
   * The returned cursor will also be managed through the means of {@link #managedCursor(Cursor)}
   *
   * @return a new {@link Cursor}
   */
  @Override
  public final T openCursor() {
    T cursor = delegate.openCursor();
    T managedCursor = managedCursor(cursor);
    cursors.add(managedCursor);

    statistics.incrementOpenCursors();
    return managedCursor;
  }

  public CursorProvider<T> getDelegate() {
    return delegate;
  }

  /**
   * Returns a managed version of the {@code cursor}. How will that cursor be managed depends on each
   * implementation. Although it is possible that the same input {@code cursor} is returned, the assumption
   * should be that a new instance will be returned.
   *
   * @param cursor the cursor to manage
   * @return a managed {@link Cursor}
   */
  protected abstract T managedCursor(T cursor);

  @Override
  public final void releaseResources() {
    janitor.releaseResources();
  }

  public CursorProviderJanitor getJanitor() {
    return janitor;
  }

  @Override
  public void close() {
    janitor.close();
  }

  @Override
  public boolean isClosed() {
    return delegate.isClosed();
  }
}
