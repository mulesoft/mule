/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CursorProviderJanitor {

  private static final Logger LOGGER = LoggerFactory.getLogger(CursorProviderJanitor.class);

  private final CursorProvider delegate;
  private final Set<Cursor> cursors;
  private final MutableStreamingStatistics statistics;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicBoolean released = new AtomicBoolean(false);

  public CursorProviderJanitor(CursorProvider delegate, Set<Cursor> cursors,
                               MutableStreamingStatistics statistics) {
    this.delegate = delegate;
    this.cursors = cursors;
    this.statistics = statistics;
  }

  public void close() {
    if (closed.compareAndSet(false, true)) {
      try {
        delegate.close();
      } finally {
        statistics.decrementOpenProviders();
      }
    }
  }

  public final void releaseResources() {
    if (!released.compareAndSet(false, true)) {
      return;
    }

    try {
      close();
    } catch (Exception e) {
      LOGGER.warn("Exception was found trying to close CursorProvider. Will try to release its resources anyway. Error was: " +
                      e.getMessage(), e);
    }

    try {
      cursors.forEach(this::releaseCursor);
    } finally {
      delegate.releaseResources();
    }
  }

  public void releaseCursor(Cursor cursor) {
    try {
      cursor.release();
    } catch (Exception e) {
      LOGGER.warn("Exception was found trying to release cursor resources. Execution will continue. Error was: " +
                      e.getMessage(), e);
    } finally {
      statistics.decrementOpenCursors();
    }
  }
}
