/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;

import java.io.IOException;

/**
 * Base class for implementations of {@link CursorStream}.
 * <p>
 * Provides template methods and enforces default behavior.
 *
 * @since 4.0
 */
abstract class AbstractCursorStream extends CursorStream {

  private final CursorStreamProvider provider;
  private long mark = 0;
  private boolean released = false;
  protected long position = 0;

  public AbstractCursorStream(CursorStreamProvider provider) {
    this.provider = provider;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getPosition() {
    return position;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void seek(long position) throws IOException {
    assertNotDisposed();
    this.position = position;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isReleased() {
    return released;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized final void release() {
    if (!released) {
      released = true;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorStreamProvider getProvider() {
    return provider;
  }

  protected void assertNotDisposed() throws IOException {
    if (released) {
      throw new IOException("Stream is closed");
    }
  }

  /**
   * Closes this stream and invokes the closing callback received in the constructor.
   */
  @Override
  public final void close() throws IOException {
    if (!released) {
      release();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException if {@code this} instance has been disposed
   */
  @Override
  public final int read() throws IOException {
    assertNotDisposed();
    return doRead();
  }

  /**
   * Template method to support the {@link #read()} method.
   *
   * @return the read byte or {@code -1} if no more elements are present in the stream
   * @throws IOException
   */
  protected abstract int doRead() throws IOException;

  /**
   * {@inheritDoc}
   */
  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    assertNotDisposed();
    return doRead(b, off, len);
  }

  /**
   * Template method to support the {@link #read(byte[], int, int)} method
   *
   * @param b   the buffer into which the data is read.
   * @param off the start offset in array <code>b</code> at which the data is written.
   * @param len the maximum number of bytes to read.
   * @return the total number of bytes read into the buffer, or {@code -1} if there is no more data because the end of
   * the stream has been reached.
   * @throws IOException
   */
  protected abstract int doRead(byte[] b, int off, int len) throws IOException;

  /**
   * {@inheritDoc}
   * Equivalent to {@code this.seek(this.getPosition() + n)}
   */
  @Override
  public final long skip(long n) throws IOException {
    seek(getPosition() + n);
    return n;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void mark(int readlimit) {
    mark = readlimit;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void reset() throws IOException {
    seek(mark);
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code true}
   */
  @Override
  public boolean markSupported() {
    return true;
  }

  protected int unsigned(int value) {
    return value & 0xff;
  }
}
