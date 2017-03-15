/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static org.mule.runtime.api.util.DataUnit.BYTE;
import org.mule.runtime.api.streaming.exception.StreamingBufferSizeExceededException;
import org.mule.runtime.api.util.DataSize;
import org.mule.runtime.core.streaming.bytes.InMemoryCursorStreamConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * An implementation of {@link AbstractInputStreamBuffer} which holds the buffered
 * information in memory.
 *
 * If the buffer does not have enough capacity to hold all the data, then it will
 * expanded up to a certain threshold configured in the constructor. Once that threshold
 * is reached, a {@link StreamingBufferSizeExceededException} will be thrown. If no threshold
 * is provided, then the buffer will be allowed to grow indefinitely.
 *
 * @since 4.0
 */
public class InMemoryStreamBuffer extends AbstractInputStreamBuffer {

  private final DataSize bufferSizeIncrement;
  private final DataSize maxBufferSize;

  /**
   * Creates a new instance
   * @param stream the stream to be buffered
   * @param config this buffer's configuration.
   */
  public InMemoryStreamBuffer(InputStream stream, InMemoryCursorStreamConfig config, ByteBufferManager bufferManager) {
    super(stream, bufferManager, config.getInitialBufferSize().toBytes());

    this.bufferSizeIncrement = config.getBufferSizeIncrement() != null
        ? config.getBufferSizeIncrement()
        : new DataSize(0, BYTE);

    this.maxBufferSize = config.getMaxInMemorySize();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void doClose() {
    // no - op
  }

  @Override
  public int getBackwardsData(ByteBuffer dest, Range requiredRange, int length) {
    return copy(dest, requiredRange);
  }

  /**
   * {@inheritDoc}
   * If {@code buffer} doesn't have any remaining capacity, then {@link #expandBuffer(int)}
   * is invoked before attempting to consume new information.
   * @throws StreamingBufferSizeExceededException if the buffer is not big enough and cannot be expanded
   */
  @Override
  public int consumeForwardData(ByteBuffer buffer) throws IOException {
    if (!buffer.hasRemaining()) {
      buffer = expandBuffer(buffer.capacity());
    }

    return loadFromStream(buffer);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ByteBuffer expandBuffer(int bytesIncrement) {
    if (!canBeExpanded()) {
      throw new StreamingBufferSizeExceededException(maxBufferSize.toBytes());
    }

    return super.expandBuffer(bytesIncrement);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean canBeExpanded() {
    if (bufferSizeIncrement.getSize() <= 0) {
      return false;
    } else if (maxBufferSize == null) {
      return true;
    }

    return getExpandedBufferSize(bufferSizeIncrement.toBytes()) <= maxBufferSize.toBytes();
  }
}
