/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming;

import org.mule.runtime.core.api.streaming.StreamingStatistics;

/**
 * Augmented version of the {@link StreamingStatistics} contract which allows to manipulate the held values.
 *
 * @since 4.2.0
 */
public interface MutableStreamingStatistics extends StreamingStatistics {

  /**
   * Increases the number of open providers by one and returns the updated value
   *
   * @return the updated value
   */
  int incrementOpenProviders();

  /**
   * Decreases the number of open providers by one and returns the updated value
   *
   * @return the updated value
   */
  int decrementOpenProviders();

  /**
   * Increases the number of open cursors by one and returns the updated value
   *
   * @return the updated value
   */
  int incrementOpenCursors();

  /**
   * Decreases the number of open cursors by one and returns the updated value
   *
   * @return the updated value
   */
  int decrementOpenCursors();

  /**
   * Decreases the number of cursors by {@code howMany} and returns the updated value
   *
   * @param howMany how many cursors were closed
   * @return the updated value
   * @since 4.3.0
   */
  int decrementOpenCursors(int howMany);
}
