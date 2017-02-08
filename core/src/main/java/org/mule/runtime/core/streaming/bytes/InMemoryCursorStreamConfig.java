/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.streaming.bytes;

import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_STREAMING_BUFFER_DATA_UNIT;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_STREAMING_BUFFER_SIZE;
import org.mule.runtime.api.util.DataSize;
import org.mule.runtime.core.internal.streaming.bytes.InputStreamBuffer;

/**
 * Configuration for a {@link InputStreamBuffer} which uses memory for buffering
 *
 * @since 4.0
 */
public final class InMemoryCursorStreamConfig {

  private final DataSize initialBufferSize;
  private final DataSize bufferSizeIncrement;
  private final DataSize maxInMemorySize;

  /**
   * @return A new instance configured with default settings
   */
  public static InMemoryCursorStreamConfig getDefault() {
    DataSize dataSize = new DataSize(DEFAULT_STREAMING_BUFFER_SIZE, DEFAULT_STREAMING_BUFFER_DATA_UNIT);
    return new InMemoryCursorStreamConfig(dataSize, dataSize, null);
  }

  /**
   * Creates a new instance
   *
   * @param initialBufferSize   the buffer's initial size
   * @param bufferSizeIncrement the size that the buffer should gain each time it is expanded
   * @param maxInMemorySize     the maximum amount of space that the buffer can grow to. Use {@code null} for unbounded buffers
   */
  public InMemoryCursorStreamConfig(DataSize initialBufferSize, DataSize bufferSizeIncrement, DataSize maxInMemorySize) {
    this.initialBufferSize = initialBufferSize;
    this.bufferSizeIncrement = bufferSizeIncrement;
    this.maxInMemorySize = maxInMemorySize;
  }

  public DataSize getInitialBufferSize() {
    return initialBufferSize;
  }

  public DataSize getBufferSizeIncrement() {
    return bufferSizeIncrement;
  }

  public DataSize getMaxInMemorySize() {
    return maxInMemorySize;
  }
}
