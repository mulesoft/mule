/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.runtime.core.streaming.bytes.ByteStreamingStatistics;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of {@link ByteStreamingStatistics}
 *
 * @since 4.0
 */
public class DefaultByteStreamingStatistics implements ByteStreamingStatistics {

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
