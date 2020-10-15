/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.management.stats.CursorComponentDecoratorFactory;

/**
 * A cursor stream provider that decorates its streams
 *
 * @since 4.4, 4.3.1
 */
public class InputDecoratedCursorStreamProvider implements CursorStreamProvider {

  private final CursorStreamProvider delegate;
  private final CursorComponentDecoratorFactory decoratorFactory;
  private final String correlationId;

  public InputDecoratedCursorStreamProvider(CursorStreamProvider delegate, CursorComponentDecoratorFactory decoratorFactory,
                                            String correlationId) {
    this.delegate = delegate;
    this.decoratorFactory = decoratorFactory;
    this.correlationId = correlationId;
  }

  @Override
  public CursorStream openCursor() {
    return decoratorFactory.decorateInput(delegate.openCursor(), correlationId);
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
  public boolean isManaged() {
    return delegate.isManaged();
  }
}
