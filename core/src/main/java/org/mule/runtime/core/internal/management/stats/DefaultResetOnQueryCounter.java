/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.core.api.management.stats.ResetOnQueryCounter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of {@link ResetOnQueryCounter} that holds the counter in an {@link AtomicLong}.
 * 
 * @since 4.5
 */
public class DefaultResetOnQueryCounter implements ResetOnQueryCounter {

  private final AtomicLong counter = new AtomicLong(0);

  @Override
  public long getAndReset() {
    return counter.getAndSet(0);
  }

  @Override
  public long get() {
    return counter.get();
  }

  public void increment() {
    counter.incrementAndGet();
  }

  public void add(long value) {
    counter.addAndGet(value);
  }
}
