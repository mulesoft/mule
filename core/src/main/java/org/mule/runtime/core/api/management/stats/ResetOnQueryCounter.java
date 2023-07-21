/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.management.stats;

/**
 * Provides a counter that can be reseted on a value fetch, which is useful for getting statistics of a given time frame if
 * consumed periodically.
 * <p>
 * Multiple instances obtained from the same statistics object are independent.
 * <p>
 * Implementations are thread-safe.
 * 
 * @since 4.5
 */
public interface ResetOnQueryCounter {

  /**
   * Returns the value of the counted events from the creation of this object or the last call to {@link #getAndReset()},
   * whichever happened last. Then sets the counter value to {@code 0}.
   * 
   * @return the current count.
   */
  long getAndReset();

  /**
   * Returns the value of the counted events from the creation of this object or the last call to {@link #getAndReset()},
   * whichever happened last.
   * 
   * @return the current count.
   */
  long get();
}
