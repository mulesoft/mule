/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static org.mule.runtime.api.util.DataUnit.BYTE;
import static org.mule.runtime.core.internal.streaming.bytes.ByteStreamingConstants.DEFAULT_BUFFER_BUCKET_SIZE;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_BYTES_STREAMING_MAX_BUFFER_SIZE;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_BYTE_STREAMING_BUFFER_DATA_UNIT;

import static java.lang.Math.max;
import static java.lang.Math.min;

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
  private final boolean eagerRead;

  /**
   * @return A new instance configured with default settings
   */
  public static FileStoreCursorStreamConfig getDefault() {
    return getDefault(false);
  }

  /**
   * @return A new instance configured with default settings
   */
  public static FileStoreCursorStreamConfig getDefault(boolean eagerRead) {
    return new FileStoreCursorStreamConfig(new DataSize(DEFAULT_BYTES_STREAMING_MAX_BUFFER_SIZE,
                                                        DEFAULT_BYTE_STREAMING_BUFFER_DATA_UNIT),
                                           eagerRead);
  }

  /**
   * Creates a new instance
   * 
   * @param maxInMemorySize the maximum amount of data to be held in memory
   */
  public FileStoreCursorStreamConfig(DataSize maxInMemorySize) {
    this(maxInMemorySize, false);
  }

  /**
   * Creates a new instance
   *
   * @param maxInMemorySize the maximum amount of data to be held in memory
   * @param eagerRead       if provided cursors read method will return immediately after reading some data
   */
  public FileStoreCursorStreamConfig(DataSize maxInMemorySize,
                                     boolean eagerRead) {
    int effectiveMaxInMemorySize = maxInMemorySize.toBytes();
    bucketSize = min(DEFAULT_BUFFER_BUCKET_SIZE, effectiveMaxInMemorySize);
    bucketsCount = max(1, effectiveMaxInMemorySize / bucketSize);
    if (effectiveMaxInMemorySize % bucketSize > 0) {
      effectiveMaxInMemorySize = bucketsCount * bucketSize;
    }
    this.maxInMemorySize = new DataSize(effectiveMaxInMemorySize, BYTE);
    this.eagerRead = eagerRead;
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

  /**
   * If eager read is {@code true}, {@code read} methods will return immediately after readily available data has been read. As
   * more data becomes available, subsequent calls to {@code read} will be required to consume that.
   * <p>
   * If eager read is {@code false}, {@code read} methods will not return until the intermediate buffer of the cursor is full.
   *
   * @return if provided cursors {@code read} methods will return immediately after readily available data has been read.
   *
   * @since 4.10.0, 4.9.7, 4.6.20, 4.4.1
   */
  public boolean isEagerRead() {
    return eagerRead;
  }
}
