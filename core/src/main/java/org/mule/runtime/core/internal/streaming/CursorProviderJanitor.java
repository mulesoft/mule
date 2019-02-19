/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

/**
 * Performs cleanup tasks for one particular {@link CursorProvider} passed in the constructor.
 * <p>
 * None of the methods in this class fail. Any exceptions are logged only.
 *
 * @since 4.2.0
 */
public class CursorProviderJanitor {

  private static final Logger LOGGER = getLogger(CursorProviderJanitor.class);

  CursorProvider provider;
  private final Set<Cursor> cursors;
  private final MutableStreamingStatistics statistics;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicBoolean released = new AtomicBoolean(false);

  /**
   * Creates a new instance
   *
   * @param provider   the {@link CursorProvider} which resources are freed
   * @param cursors    a {@link Set} with the providers open cursors
   * @param statistics a {@link MutableStreamingStatistics}
   */
  public CursorProviderJanitor(CursorProvider provider, Set<Cursor> cursors, MutableStreamingStatistics statistics) {
    this.provider = provider;
    this.cursors = cursors;
    this.statistics = statistics;
  }

  /**
   * Closes the underlying {@link CursorProvider}
   */
  public void close() {
    if (closed.compareAndSet(false, true)) {
      try {
        provider.close();
      } finally {
        statistics.decrementOpenProviders();
      }
    }
  }

  /**
   * Releases the resources of the underlying {@link CursorProvider}, including its {@link Cursor cursors}
   */
  public final void releaseResources() {
    if (!released.compareAndSet(false, true)) {
      return;
    }

    try {
      close();
    } catch (Exception e) {
      LOGGER.warn("Exception was found trying to close CursorProvider. Will try to release its resources anyway", e);
    }

    try {
      cursors.forEach(this::releaseCursor);
    } finally {
      provider.releaseResources();
      cursors.clear();
      provider = null;
    }
  }

  /**
   * Releases the resources associated to the given {@code cursor}.
   *
   * @param cursor a {@link Cursor}
   */
  public void releaseCursor(Cursor cursor) {
    try {
      cursor.release();
    } catch (Exception e) {
      LOGGER.warn("Exception was found trying to release cursor resources. Execution will continue", e);
    } finally {
      statistics.decrementOpenCursors();
    }
  }
}
