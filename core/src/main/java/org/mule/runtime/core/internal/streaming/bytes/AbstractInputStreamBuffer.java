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
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.util.func.CheckedRunnable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;

/**
 * Base class for implementations of {@link InputStreamBuffer}.
 * <p>
 * Contains the base algorithm and template methods so that implementations can be created easily
 *
 * @since 4.0
 */
public abstract class AbstractInputStreamBuffer implements InputStreamBuffer {

  private static Logger LOGGER = getLogger(AbstractInputStreamBuffer.class);

  private final Lock bufferLock = new ReentrantLock();

  private InputStream stream;
  private final ByteBufferManager bufferManager;
  private ReadableByteChannel streamChannel;
  private ByteBuffer buffer;
  private boolean closed = false;
  private Range bufferRange;
  private boolean streamFullyConsumed = false;

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

    bufferRange = new Range(0, 0);
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
   * @param buffer the buffer in which the data is to be loaded. The buffer must be in the correct position to receive the data
   *               and have enough space remaining
   * @return the amount of bytes read
   * @throws IOException
   */
  public abstract int consumeForwardData(ByteBuffer buffer) throws IOException;

  /**
   * Re obtains information which has already been consumed and this buffer is keeping somehow. This method will be invoked
   * when the cursor is rewind.
   *
   * @param dest          the buffer in which the data is to be loaded. The buffer must be in the correct position to receive the data
   *                      and have enough space remaining
   * @param requiredRange the range of required information
   * @param length        the amount of information to read
   * @return the amount of bytes actually read
   */
  public abstract int getBackwardsData(ByteBuffer dest, Range requiredRange, int length);

  /**
   * @return Whether this buffer can be expanded
   */
  protected abstract boolean canBeExpanded();

  /**
   * @return the {@link ByteBufferManager} that <b>MUST</b> to be used to allocate byte buffers
   */
  protected ByteBufferManager getBufferManager() {
    return bufferManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void close() {
    closed = true;

    doClose();

    if (streamChannel != null) {
      safely(streamChannel::close);
    }

    if (stream != null) {
      safely(stream::close);
    }

    deallocate(buffer);
  }

  /**
   * Template method to support the {@link #close()} operation
   */
  public abstract void doClose();

  /**
   * Looses the reference to the {@link #stream}, {@link #streamChannel} and {@link #buffer} so that invoking
   * the {@link #close()} method does not close them. This is useful when the stream is going to continue
   * buffering through a different instance.
   */
  public void yield() {
    streamChannel = null;
    stream = null;
    buffer = null;
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException if the buffer is closed
   */
  @Override
  public final int get(ByteBuffer destination, long position, int length) {
    checkState(!closed, "Buffer is closed");

    return doGet(destination, position, length, true);
  }

  private int doGet(ByteBuffer dest, long position, int length, boolean consumeStreamIfNecessary) {
    Range requiredRange = new Range(position, position + length);

    acquireBufferLock();

    try {

      if (streamFullyConsumed && requiredRange.startsAfter(bufferRange)) {
        return -1;
      }

      if (bufferRange.contains(requiredRange)) {
        return copy(dest, requiredRange);
      }

      if (bufferRange.isAhead(requiredRange)) {
        return getBackwardsData(dest, requiredRange, length);
      }

      int overlap = handlePartialOverlap(dest, requiredRange);
      if (overlap > 0) {
        return overlap;
      }

      if (consumeStreamIfNecessary) {
        while (!streamFullyConsumed && bufferRange.isBehind(requiredRange)) {
          try {
            if (reloadBuffer() > 0) {
              overlap = handlePartialOverlap(dest, requiredRange);
              if (overlap > 0) {
                return overlap;
              }
            }
          } catch (IOException e) {
            throw new MuleRuntimeException(createStaticMessage("Could not read stream"), e);
          }
        }

        return doGet(dest, position, length, false);
      } else {
        return handlePartialOverlap(dest, requiredRange);
      }
    } finally {
      releaseBufferLock();
    }
  }

  protected void consume(ByteBuffer data) {
    int read = data.remaining();
    if (read > 0) {
      buffer.put(data);
      bufferRange = bufferRange.advance(read);
    }
  }

  /**
   * Releases the lock obtained through {@link #acquireBufferLock()}
   */
  public void releaseBufferLock() {
    try {
      bufferLock.unlock();
    } catch (IllegalMonitorStateException e) {
      // lock was released early to improve performance and somebody else took it. This is fine
    }
  }

  /**
   * Acquires an exclusive lock to the {@link #buffer}
   */
  public void acquireBufferLock() {
    bufferLock.lock();
  }

  /**
   * @return the {@link #buffer}
   */
  public ByteBuffer getBuffer() {
    return buffer;
  }

  private int reloadBuffer() throws IOException {
    if (streamFullyConsumed) {
      return -1;
    }

    int result = consumeForwardData(buffer);

    if (result >= 0) {
      bufferRange = bufferRange.advance(result);
    } else {
      streamFullyConsumed();
    }

    return result;
  }

  protected int loadFromStream(ByteBuffer buffer) throws IOException {
    int result;
    try {
      result = streamChannel.read(buffer);
    } catch (ClosedChannelException e) {
      result = -1;
    }
    return result;
  }

  /**
   * Expands the size of the buffer by {@code bytesIncrement}
   *
   * @param bytesIncrement how many bytes to gain
   * @return a new, expanded {@link ByteBuffer}
   */
  protected ByteBuffer expandBuffer(int bytesIncrement) {
    ByteBuffer newBuffer = bufferManager.allocate(getExpandedBufferSize(bytesIncrement));
    buffer.position(0);
    newBuffer.put(buffer);
    ByteBuffer oldBuffer = buffer;
    buffer = newBuffer;
    deallocate(oldBuffer);

    return buffer;
  }

  protected void deallocate(ByteBuffer byteBuffer) {
    if (byteBuffer != null) {
      safely(() -> bufferManager.deallocate(byteBuffer));
    }
  }

  /**
   * @return the buffered {@link InputStream}
   */
  protected InputStream getStream() {
    return stream;
  }

  /**
   * @return the {@link #streamChannel}
   */
  protected ReadableByteChannel getStreamChannel() {
    return streamChannel;
  }

  protected int getExpandedBufferSize(int bytesIncrement) {
    return buffer.capacity() + bytesIncrement;
  }

  protected void streamFullyConsumed() {
    streamFullyConsumed = true;
  }

  private int handlePartialOverlap(ByteBuffer dest, Range requiredRange) {
    return bufferRange.overlap(requiredRange)
        .filter(r -> !r.isEmpty())
        .map(overlap -> copy(dest, overlap))
        .orElse(-1);
  }

  protected int copy(ByteBuffer dest, Range requiredRange) {
    ByteBuffer src = buffer.duplicate();

    final int newPosition;
    if (requiredRange.getStart() >= buffer.limit()) {
      newPosition = toIntExact(requiredRange.getStart() - bufferRange.getStart());
    } else {
      newPosition = toIntExact(requiredRange.getStart());
    }

    src.position(newPosition);
    src.limit(newPosition + min(dest.remaining(), min(requiredRange.length(), src.remaining())));
    if (src.hasRemaining()) {
      int remaining = src.remaining();
      dest.put(src);
      return remaining;
    } else {
      return -1;
    }
  }

  private void safely(CheckedRunnable task) {
    try {
      task.run();
    } catch (Exception e) {
      LOGGER.debug("Found exception closing buffer", e);
    }
  }
}
