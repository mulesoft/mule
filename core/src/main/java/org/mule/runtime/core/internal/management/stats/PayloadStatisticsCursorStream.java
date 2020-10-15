/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import java.io.IOException;
import java.util.function.LongConsumer;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStream;

final class PayloadStatisticsCursorStream extends CursorStream {

  private final LongConsumer populator;
  private CursorStream delegate;

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
    return delegate.getProvider();
  }

  @Override
  public int read() throws IOException {
    final int read = delegate.read();
    if (read != -1) {
      populator.accept(1);
    }

    return read;
  }

}
