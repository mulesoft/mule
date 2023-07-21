/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.streaming;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;

/**
 * Statistics about current streaming assets
 *
 * @since 4.0
 */
@NoImplement
public interface StreamingStatistics {

  /**
   * @return How many {@link CursorStreamProvider} instances are currently open
   */
  int getOpenCursorProvidersCount();

  /**
   * @return How many {@link CursorStream} instances are currently open
   */
  int getOpenCursorsCount();
}
