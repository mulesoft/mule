/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.lang.Math.min;
import static java.lang.Math.toIntExact;
import static java.nio.channels.Channels.newChannel;
import static org.mule.runtime.api.util.Preconditions.checkState;
import org.mule.runtime.core.internal.streaming.AbstractStreamingBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;

/**
 * Base class for implementations of {@link InputStreamBuffer}.
 * <p>
 * Contains the base algorithm and template methods so that implementations can be created easily
 *
 * @since 4.0
 */
public abstract class AbstractInputStreamBuffer extends AbstractStreamingBuffer implements InputStreamBuffer {

  protected final ByteBufferManager bufferManager;

  private InputStream stream;
  private ReadableByteChannel streamChannel;
  private boolean streamFullyConsumed = false;
  protected ByteBuffer buffer;

  /**
   * Creates a new instance
   *
   * @param stream        The stream being buffered. This is the original data source
   * @param bufferSize    the buffer size
   * @param bufferManager the {@link ByteBufferManager} that will be used to allocate all buffers
   */
  public AbstractInputStreamBuffer(InputStream stream, ByteBufferManager bufferManager, int bufferSize) {
    this(stream, openStreamChannel(stream), bufferManager, bufferSize);
  }

  /**
   * Creates a new instance
   *
   * @param stream        The stream being buffered. This is the original data source
   * @param streamChannel a {@link ReadableByteChannel} used to read from the {@code stream}
   * @param bufferManager the {@link ByteBufferManager} that will be used to allocate all buffers
   * @param bufferSize    the buffer size
   */
  public AbstractInputStreamBuffer(InputStream stream, ReadableByteChannel streamChannel, ByteBufferManager bufferManager,
                                   int bufferSize) {
    this(stream, streamChannel, bufferManager, bufferManager.allocate(bufferSize));
  }

  /**
   * Creates a new instance
   *
   * @param stream        The stream being buffered. This is the original data source
   * @param streamChannel a {@link ReadableByteChannel} used to read from the {@code stream}
   * @param bufferManager the {@link ByteBufferManager} that will be used to allocate all buffers
   * @param buffer        the buffer to use
   */
  public AbstractInputStreamBuffer(InputStream stream, ReadableByteChannel streamChannel, ByteBufferManager bufferManager,
                                   ByteBuffer buffer) {
    this.stream = stream;
    this.streamChannel = streamChannel;
    this.bufferManager = bufferManager;
    this.buffer = buffer;
  }

  /**
   * @param stream the stream to consume
   * @return a new {@link ReadableByteChannel} for consuming the {@code stream}
   */
  protected static ReadableByteChannel openStreamChannel(InputStream stream) {
    return stream != null ? newChannel(stream) : null;
  }

  /**
   * Consumes the stream in order to obtain data that has not been read yet.
   *
   * @return the amount of bytes read
   * @throws IOException
   */
  public abstract int consumeForwardData() throws IOException;

  /**
   * {@inheritDoc}
   */
  @Override
  public final void close() {
    if (closed.compareAndSet(false, true)) {
      withWriteLock(() -> {
        try {
          doClose();
          return null;
        } finally {
          if (streamChannel != null) {
            closeSafely(streamChannel::close);
          }

          if (stream != null) {
            closeSafely(stream::close);
          }

          deallocate(buffer);
        }
      });
    }
  }

  /**
   * Template method to support the {@link #close()} operation
   */
  public abstract void doClose();

  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException if the buffer is closed
   */
  @Override
  public final ByteBuffer get(long position, int length) {
    checkState(!closed.get(), "Buffer is closed");
    return doGet(position, length);
  }

  protected abstract ByteBuffer doGet(long position, int length);

  protected void consume(ByteBuffer data) {
    int read = data.remaining();
    if (read > 0) {
      buffer.put(data);
    }
  }

  /**
   * @return the {@link #buffer}
   */
  public ByteBuffer getBuffer() {
    return buffer;
  }

  protected int consumeStream(ByteBuffer buffer) throws IOException {
    int result;
    try {
      result = streamChannel.read(buffer);
    } catch (ClosedChannelException e) {
      result = -1;
    }
    return result;
  }

  protected void deallocate(ByteBuffer byteBuffer) {
    if (byteBuffer != null) {
      closeSafely(() -> bufferManager.deallocate(byteBuffer));
    }
  }

  protected boolean isStreamFullyConsumed() {
    return streamFullyConsumed;
  }

  protected void streamFullyConsumed() {
    streamFullyConsumed = true;
  }


  protected ByteBuffer copy(long position, int length) {
    ByteBuffer copy = buffer.duplicate();

    final int newPosition = toIntExact(position);

    copy.position(newPosition);
    copy.limit(newPosition + min(copy.capacity(), min(length, copy.remaining())));

    return copy;
  }
}
