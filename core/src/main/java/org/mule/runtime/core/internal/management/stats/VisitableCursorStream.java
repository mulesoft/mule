/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import java.io.IOException;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.core.internal.management.stats.visitor.Visitable;
import org.mule.runtime.core.internal.management.stats.visitor.Visitor;

/**
 * An cursor stream that can be visit to decorate.
 *
 * @since 4.4, 4.3.1
 */
public class VisitableCursorStream extends CursorStream implements Visitable<CursorStream> {

  private final CursorStream delegate;

  public VisitableCursorStream(CursorStream delegate) {
    this.delegate = delegate;
  }

  @Override
  public CursorStream accept(Visitor<CursorStream> visitor) {
    return visitor.visitCursorStream(this);
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
  public CursorStream getDelegate() {
    return delegate;
  }

  @Override
  public int read() throws IOException {
    return delegate.read();
  }

}
