/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.nio.channels.Channels.newChannel;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.core.api.streaming.bytes.ByteBufferManager;
import org.mule.runtime.core.internal.streaming.AbstractStreamingBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;

import org.slf4j.Logger;

/**
 * Base class for implementations of {@link InputStreamBuffer}.
 * <p>
 * Contains the base algorithm and template methods so that implementations can be created easily
 *
 * @since 4.0
 */
public abstract class AbstractInputStreamBuffer extends AbstractStreamingBuffer implements InputStreamBuffer {

  private static final Logger LOGGER = getLogger(AbstractInputStreamBuffer.class);

  protected final ByteBufferManager bufferManager;

  private final InputStream stream;
  private ReadableByteChannel streamChannel;
  private boolean streamFullyConsumed = false;

  /**
   * Creates a new instance
   *
   * @param stream The stream being buffered. This is the original data source
   * @param bufferManager the {@link ByteBufferManager} that will be used to allocate all buffers
   */
  public AbstractInputStreamBuffer(InputStream stream, ByteBufferManager bufferManager) {
    this(stream, openStreamChannel(stream), bufferManager);
  }

  /**
   * Creates a new instance
   *
   * @param stream The stream being buffered. This is the original data source
   * @param streamChannel a {@link ReadableByteChannel} used to read from the {@code stream}
   * @param bufferManager the {@link ByteBufferManager} that will be used to allocate all buffers
   */
  public AbstractInputStreamBuffer(InputStream stream, ReadableByteChannel streamChannel, ByteBufferManager bufferManager) {
    this.stream = stream;
    this.streamChannel = streamChannel;
    this.bufferManager = bufferManager;
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

  protected int consumeStream(ByteBuffer buffer) throws IOException {
    int result;
    try {
      result = streamChannel.read(buffer);
    } catch (ClosedByInterruptException cbie) {
      LOGGER.error("Channel interrupted.", cbie);
      throw cbie;
    } catch (AsynchronousCloseException ace) {
      LOGGER.error("Channel closed asynchronously.", ace);
      throw ace;
    } catch (ClosedChannelException cce) {
      // Assume ClosedChannelException means there is no more data. AsynchronousCloseException which are not expected are handled
      // separably above.
      LOGGER.debug("Channel closed.", cce);
      result = -1;
    }
    return result;
  }

  protected boolean deallocate(ByteBuffer byteBuffer) {
    if (byteBuffer != null) {
      closeSafely(() -> bufferManager.deallocate(byteBuffer));
      return true;
    }

    return false;
  }

  protected boolean isStreamFullyConsumed() {
    return streamFullyConsumed;
  }

  protected void streamFullyConsumed() {
    streamFullyConsumed = true;
  }

  protected abstract ByteBuffer copy(long position, int length);
}
