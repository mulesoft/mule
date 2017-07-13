/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import org.mule.runtime.core.api.streaming.StreamingStatistics;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mutable implementation of {@link StreamingStatistics}
 *
 * @since 4.0
 */
public class MutableStreamingStatistics implements StreamingStatistics {

  private final AtomicInteger openProviders = new AtomicInteger(0);
  private final AtomicInteger openCursors = new AtomicInteger(0);

  void incrementOpenProviders() {
    openProviders.incrementAndGet();
  }

  void decrementOpenProviders() {
    openProviders.decrementAndGet();
  }

  void incrementOpenCursors() {
    openCursors.incrementAndGet();
  }

  void decrementOpenCursors() {
    openCursors.decrementAndGet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getOpenCursorProvidersCount() {
    return openProviders.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getOpenCursorsCount() {
    return openCursors.get();
  }
}
