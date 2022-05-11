/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message.stream;

import java.io.IOException;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStream;

/**
 * Implementation of {@link CursorStream} that decorates a {@link CursorStream} so that it cannot be closed . For example, this
 * implementation can be used in operation method parameters, where we don't want the extension developer to close the underlying
 * {@link CursorStream}
 */
public class UnclosableCursorStream extends CursorStream {

  private CursorStream delegate;

  public UnclosableCursorStream(CursorStream delegate) {
    this.delegate = delegate;
  }

  @Override
  public int read() throws IOException {
    return delegate.read();
  }

  @Override
  public int read(byte[] b) throws IOException {
    return delegate.read(b);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return delegate.read(b, off, len);
  }

  @Override
  public long skip(long n) throws IOException {
    return delegate.skip(n);
  }

  @Override
  public int available() throws IOException {
    return delegate.available();
  }

  @Override
  public synchronized void mark(int readlimit) {
    delegate.mark(readlimit);
  }

  @Override
  public synchronized void reset() throws IOException {
    delegate.reset();
  }

  @Override
  public boolean markSupported() {
    return super.markSupported();
  }

  @Override
  public long getPosition() {
    return delegate.getPosition();
  }

  @Override
  public void seek(long position) throws IOException {
    delegate.seek(position);
  }

  @Override
  public void release() {
    delegate.release();
  }

  @Override
  public boolean isReleased() {
    return delegate.isReleased();
  }

  @Override
  public CursorProvider getProvider() {
    return delegate.getProvider();
  }

  @Override
  public void close() throws IOException {
    /**
     * This implementation of InputStream does nothing on the close method, this is because we don't want extensions developers to
     * close the CursorStream we are providing without them knowing. The runtime with take care of closing the underlying stream.
     */
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
