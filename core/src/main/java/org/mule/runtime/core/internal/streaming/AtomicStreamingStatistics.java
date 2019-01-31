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
public class AtomicStreamingStatistics implements MutableStreamingStatistics {

  private final AtomicInteger openProviders = new AtomicInteger(0);
  private final AtomicInteger openCursors = new AtomicInteger(0);

  @Override
  public int incrementOpenProviders() {
    return openProviders.incrementAndGet();
  }

  @Override
  public int decrementOpenProviders() {
    return openProviders.decrementAndGet();
  }

  @Override
  public int incrementOpenCursors() {
    return openCursors.incrementAndGet();
  }

  @Override
  public int decrementOpenCursors() {
    return openCursors.decrementAndGet();
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
