/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;

import java.io.IOException;
import java.util.function.LongConsumer;

final class PayloadStatisticsCursorStream extends CursorStream {

  private final LongConsumer populator;
  private final CursorStream delegate;

  PayloadStatisticsCursorStream(CursorStream delegate, LongConsumer populator) {
    this.delegate = delegate;
    this.populator = populator;
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
    return new CursorStreamProvider() {

      @Override
      public CursorStream openCursor() {
        return new PayloadStatisticsCursorStream((CursorStream) delegate.getProvider().openCursor(), populator);
      }

      @Override
      public void close() {
        delegate.getProvider().close();
      }

      @Override
      public void releaseResources() {
        delegate.getProvider().releaseResources();
      }

      @Override
      public boolean isClosed() {
        return delegate.getProvider().isClosed();
      }
    };
  }

  @Override
  public int read() throws IOException {
    final int read = delegate.read();
    if (read != -1) {
      populator.accept(1);
    }

    return read;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    final int read = delegate.read(b, off, len);
    // ignore -1 indicating no data read
    if (read > 0) {
      populator.accept(read);
    }
    return read;
  }

}
