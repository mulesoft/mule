/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static org.mule.runtime.core.internal.streaming.bytes.ByteStreamingConstants.DEFAULT_BUFFER_BUCKET_SIZE;

import static java.lang.Math.toIntExact;

import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A {@link CursorStream} which pulls its data from an {@link InputStreamBuffer}.
 * <p>
 * To reduce contention on the {@link InputStreamBuffer}, this class also uses a local intermediate memory buffer which size must
 * be configured
 *
 * @see InputStreamBuffer
 * @since 4.0
 */
public final class BufferedCursorStream extends AbstractCursorStream {

  private static final int LOCAL_BUFFER_SIZE = DEFAULT_BUFFER_BUCKET_SIZE;
  private static final ByteBuffer NULL_BUFFER = ByteBuffer.allocate(0);

  private final InputStreamBuffer streamBuffer;
  private final boolean eagerRead;

  /**
   * Intermediate buffer between this cursor and the {@code traversableBuffer}. This reduces contention on the
   * {@code traversableBuffer}
   */
  private ByteBuffer localBuffer = NULL_BUFFER;
  private long rangeStart = 0;
  private long rangeEnd = -1;

  /**
   * Creates a new instance
   *
   * @param streamBuffer the buffer which provides data
   * @param provider     the {@link CursorStreamProvider} for the {@link CursorStream cursors} that will consume this buffer
   */
  public BufferedCursorStream(InputStreamBuffer streamBuffer, CursorStreamProvider provider, boolean eagerRead) {
    super(provider);
    this.streamBuffer = streamBuffer;
    this.eagerRead = eagerRead;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void seek(long position) throws IOException {
    super.seek(position);

    boolean reset = false;
    if (position < rangeStart || position >= rangeEnd) {
      reset = true;
    } else {
      int bufferPosition = toIntExact(position - rangeStart);
      if (bufferPosition >= localBuffer.limit()) {
        reset = true;
      } else {
        localBuffer.position(bufferPosition);
      }
    }

    if (reset) {
      localBuffer.clear();
      localBuffer.flip();
      rangeStart = position;
      rangeEnd = -1;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected int doRead() throws IOException {
    if (assureDataInLocalBuffer(1) == -1) {
      return -1;
    }

    int read = unsigned(localBuffer.get());
    position++;
    return read;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected int doRead(byte[] b, int off, int len) throws IOException {
    int read = 0;

    while (true) {
      int remaining = assureDataInLocalBuffer(len);
      if (remaining == -1) {
        return read == 0 ? -1 : read;
      }

      if (len <= remaining) {
        localBuffer.get(b, off, len);
        position += len;
        return read + len;
      } else {
        localBuffer.get(b, off, remaining);
        position += remaining;
        read += remaining;
        off += remaining;
        len -= remaining;
        if (eagerRead) {
          return read;
        }
      }
    }
  }

  private int assureDataInLocalBuffer(int len) throws IOException {
    if (len <= localBuffer.remaining()) {
      return toIntExact(len);
    }

    localBuffer.clear();
    ByteBuffer read = streamBuffer.get(position, LOCAL_BUFFER_SIZE);
    if (read != null) {
      localBuffer = read;
      rangeStart = position;
      rangeEnd = rangeStart + read.remaining();

      return read.remaining();
    } else {
      localBuffer.limit(0);
      return -1;
    }
  }
}
