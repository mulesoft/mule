/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats.visitor;

import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;

/**
 * A cursor stream provider that can be visit to be decorated
 *
 * @since 4.4, 4.3.1
 */
public class VisitableCursorStreamProvider implements Visitable<CursorStreamProvider>, CursorStreamProvider {

  private CursorStreamProvider delegate;

  public VisitableCursorStreamProvider(CursorStreamProvider delegate) {
    this.delegate = delegate;
  }

  @Override
  public CursorStream openCursor() {
    return delegate.openCursor();
  }

  @Override
  public void close() {
    delegate.close();
  }

  @Override
  public void releaseResources() {
    delegate.releaseResources();
  }

  @Override
  public boolean isClosed() {
    return delegate.isClosed();
  }

  @Override
  public CursorStreamProvider accept(Visitor visitor) {
    return visitor.visitCursorStreamProvider(this);
  }

  @Override
  public CursorStreamProvider getDelegate() {
    return delegate;
  }

}
