/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.internal.streaming.CursorContext;
import org.mule.runtime.core.internal.streaming.CursorManager;
import org.mule.runtime.core.internal.streaming.ManagedCursorProvider;

import java.io.IOException;

/**
 * {@link ManagedCursorProvider} implementation for {@link CursorStreamProvider} instances
 *
 * @since 4.0
 */
public class ManagedCursorStreamProvider extends ManagedCursorProvider<CursorStream> implements CursorStreamProvider {

  /**
   * {@inheritDoc}
   */
  public ManagedCursorStreamProvider(CursorContext cursorContext, CursorManager cursorManager) {
    super(cursorContext, cursorManager);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected CursorStream managedCursor(CursorStream cursor, CursorContext handle) {
    return new ManagedCursorDecorator(cursor, handle);
  }

  private class ManagedCursorDecorator extends CursorStream {

    private final CursorStream delegate;
    private final CursorContext cursorContext;

    private ManagedCursorDecorator(CursorStream delegate, CursorContext cursorContext) {
      this.delegate = delegate;
      this.cursorContext = cursorContext;
    }

    @Override
    public void close() throws IOException {
      try {
        delegate.close();
      } finally {
        getCursorManager().onClose(delegate, cursorContext);
      }
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
    public boolean isReleased() {
      return delegate.isReleased();
    }

    @Override
    public void release() {
      delegate.release();
    }

    @Override
    public CursorProvider getProvider() {
      return delegate.getProvider();
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
    public void mark(int readlimit) {
      delegate.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
      delegate.reset();
    }

    @Override
    public boolean markSupported() {
      return delegate.markSupported();
    }
  }
}
