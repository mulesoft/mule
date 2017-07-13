/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_BYTES_STREAMING_MAX_BUFFER_SIZE;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_BYTE_STREAMING_BUFFER_DATA_UNIT;
import org.mule.runtime.api.util.DataSize;

/**
 * Configuration for a {@link InputStreamBuffer} which uses a local file for buffering
 *
 * @since 4.0
 */
public final class FileStoreCursorStreamConfig {

  private final DataSize maxInMemorySize;

  /**
   * @return A new instance configured with default settings
   */
  public static FileStoreCursorStreamConfig getDefault() {
    return new FileStoreCursorStreamConfig(new DataSize(DEFAULT_BYTES_STREAMING_MAX_BUFFER_SIZE,
                                                        DEFAULT_BYTE_STREAMING_BUFFER_DATA_UNIT));
  }

  /**
   * Creates a new instance
   * @param maxInMemorySize the maximum amount of data to be held in memory
   */
  public FileStoreCursorStreamConfig(DataSize maxInMemorySize) {
    this.maxInMemorySize = maxInMemorySize;
  }

  /**
   * @return The maximum amount of data to be held in memory
   */
  public DataSize getMaxInMemorySize() {
    return maxInMemorySize;
  }
}
