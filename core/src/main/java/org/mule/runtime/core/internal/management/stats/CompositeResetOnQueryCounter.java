/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.core.api.management.stats.ResetOnQueryCounter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of {@link ResetOnQueryCounter} that composes other counters.
 * <p>
 * Getting values will add up the values of all the held counters, and resetting this will reset the held counters as well.
 * 
 * @since 4.5
 */
public class CompositeResetOnQueryCounter implements ResetOnQueryCounter {

  private final Set<ResetOnQueryCounter> counters;

  public CompositeResetOnQueryCounter(Collection<ResetOnQueryCounter> counters) {
    this.counters = new HashSet<>();
    this.counters.addAll(counters);
  }

  @Override
  public long getAndReset() {
    long total = 0;
    for (ResetOnQueryCounter counter : counters) {
      total += counter.getAndReset();
    }
    return total;
  }

  @Override
  public long get() {
    long total = 0;
    for (ResetOnQueryCounter counter : counters) {
      total += counter.get();
    }
    return total;
  }

}
