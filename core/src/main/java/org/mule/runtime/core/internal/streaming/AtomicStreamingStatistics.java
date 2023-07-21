/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of {@link MutableStreamingStatistics} based on {@link AtomicInteger}
 *
 * @since 4.2.0
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

  @Override
  public int decrementOpenCursors(int howMany) {
    return openCursors.addAndGet(-howMany);
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
