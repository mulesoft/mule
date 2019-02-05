/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import org.mule.runtime.core.api.streaming.StreamingStatistics;

/**
 * Augmented version of the {@link StreamingStatistics} contract which allows to manipulate
 * the held values.
 *
 * @since 4.1.6
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
   * Decreases the number of open providers by one and returns the updated value
   *
   * @return the updated value
   */
  int decrementOpenCursors();
}
