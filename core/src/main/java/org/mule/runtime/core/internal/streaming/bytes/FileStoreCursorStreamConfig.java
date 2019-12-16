/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.mule.runtime.api.util.DataUnit.BYTE;
import static org.mule.runtime.core.internal.streaming.bytes.ByteStreamingConstants.DEFAULT_BUFFER_BUCKET_SIZE;
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
  private final int bucketSize;
  private final int bucketsCount;

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
    int effectiveMaxInMemorySize = maxInMemorySize.toBytes();
    bucketSize = min(DEFAULT_BUFFER_BUCKET_SIZE, effectiveMaxInMemorySize);
    bucketsCount = max(1, effectiveMaxInMemorySize / bucketSize);
    if (effectiveMaxInMemorySize % bucketSize > 0) {
      effectiveMaxInMemorySize = bucketsCount * bucketSize;
    }
    this.maxInMemorySize = new DataSize(effectiveMaxInMemorySize, BYTE);
  }

  /**
   * @return The maximum amount of data to be held in memory
   */
  public DataSize getMaxInMemorySize() {
    return maxInMemorySize;
  }

  public int getBucketSize() {
    return bucketSize;
  }

  public int getBucketsCount() {
    return bucketsCount;
  }
}
