/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming.bytes;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_BYTES_STREAMING_MAX_BUFFER_SIZE;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_BYTE_STREAMING_BUFFER_DATA_UNIT;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_BYTE_STREAMING_BUFFER_INCREMENT_SIZE;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_BYTE_STREAMING_BUFFER_SIZE;

import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.util.DataSize;

/**
 * Configuration for a {@link CursorStream} which uses memory for buffering
 *
 * @since 4.0
 */
public final class InMemoryCursorStreamConfig {

  private final DataSize initialBufferSize;
  private final DataSize bufferSizeIncrement;
  private final DataSize maxBufferSize;
  private final boolean eagerRead;

  /**
   * @return A new instance configured with default settings
   */
  public static InMemoryCursorStreamConfig getDefault() {
    return getDefault(false);
  }

  /**
   * @return A new instance configured with default settings
   */
  public static InMemoryCursorStreamConfig getDefault(final boolean eagerRead) {
    return new InMemoryCursorStreamConfig(new DataSize(DEFAULT_BYTE_STREAMING_BUFFER_SIZE,
                                                       DEFAULT_BYTE_STREAMING_BUFFER_DATA_UNIT),
                                          new DataSize(DEFAULT_BYTE_STREAMING_BUFFER_INCREMENT_SIZE,
                                                       DEFAULT_BYTE_STREAMING_BUFFER_DATA_UNIT),
                                          new DataSize(DEFAULT_BYTES_STREAMING_MAX_BUFFER_SIZE,
                                                       DEFAULT_BYTE_STREAMING_BUFFER_DATA_UNIT),
                                          eagerRead);
  }

  /**
   * Creates a new instance
   *
   * @param initialBufferSize   the buffer's initial size. Must be greater than zero bytes.
   * @param bufferSizeIncrement the size that the buffer should gain each time it is expanded. A value of zero bytes means no
   *                            expansion. Cannot be negative byte size.
   * @param maxBufferSize       the maximum amount of space that the buffer can grow to. Use {@code null} for unbounded buffers
   */
  public InMemoryCursorStreamConfig(DataSize initialBufferSize, DataSize bufferSizeIncrement, DataSize maxBufferSize) {
    this(initialBufferSize, bufferSizeIncrement, maxBufferSize, false);
  }

  /**
   * Creates a new instance
   *
   * @param initialBufferSize   the buffer's initial size. Must be greater than zero bytes.
   * @param bufferSizeIncrement the size that the buffer should gain each time it is expanded. A value of zero bytes means no
   *                            expansion. Cannot be negative byte size.
   * @param maxBufferSize       the maximum amount of space that the buffer can grow to. Use {@code null} for unbounded buffers
   * @param eagerRead           if provided cursors read method will return immediately after reading some data
   */
  public InMemoryCursorStreamConfig(DataSize initialBufferSize, DataSize bufferSizeIncrement, DataSize maxBufferSize,
                                    boolean eagerRead) {
    final int initial = initialBufferSize.toBytes();
    checkArgument(initial > 0, "initialBufferSize must be greater than zero bytes");
    final int increment = bufferSizeIncrement.toBytes();
    checkArgument(increment >= 0, "bufferSizeIncrement cannot be a negative byte size");
    final int max = maxBufferSize.toBytes();
    if (max > 0) {
      checkArgument(initial <= max, "initialBufferSize cannot be bigger than the maxBufferSize");
      checkArgument(increment <= max, "bufferSizeIncrement cannot be bigger than the maxBufferSize");
      checkArgument(initial + increment <= max,
                    "initialBufferSize + bufferSizeIncrement cannot be bigger than the maxBufferSize, "
                        + "otherwise the buffer will never be able to expand");
    }

    this.initialBufferSize = initialBufferSize;
    this.bufferSizeIncrement = bufferSizeIncrement;
    this.maxBufferSize = maxBufferSize;
    this.eagerRead = eagerRead;
  }

  public DataSize getInitialBufferSize() {
    return initialBufferSize;
  }

  public DataSize getBufferSizeIncrement() {
    return bufferSizeIncrement;
  }

  public DataSize getMaxBufferSize() {
    return maxBufferSize;
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
