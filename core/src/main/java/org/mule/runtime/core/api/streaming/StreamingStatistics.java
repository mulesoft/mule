/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.internal.streaming.NullStreamingStatistics;

/**
 * Statistics about current streaming assets
 *
 * @since 4.0
 */
@NoImplement
public interface StreamingStatistics {

  /**
   * @return a null implementation of {@link StreamingStatistics} where all methods simply return zero.
   */
  static StreamingStatistics nullStreamingStatistics() {
    return new NullStreamingStatistics();
  }

  /**
   * @return How many {@link CursorStreamProvider} instances are currently open
   */
  int getOpenCursorProvidersCount();

  /**
   * @return How many {@link CursorStream} instances are currently open
   */
  int getOpenCursorsCount();
}
