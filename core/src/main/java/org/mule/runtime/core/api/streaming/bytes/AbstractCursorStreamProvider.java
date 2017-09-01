/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming.bytes;

import static org.mule.runtime.api.util.Preconditions.checkState;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for {@link CursorStreamProvider} implementations.
 *
 * @since 4.0
 */
public abstract class AbstractCursorStreamProvider extends AbstractComponent implements CursorStreamProvider {

  protected final InputStream wrappedStream;

  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new instance
   *
   * @param wrappedStream the original stream to be decorated
   */
  public AbstractCursorStreamProvider(InputStream wrappedStream) {
    this.wrappedStream = wrappedStream;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final CursorStream openCursor() {
    checkState(!closed.get(), "Cannot open a new cursor on a closed stream");
    return doOpenCursor();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    closed.set(true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isClosed() {
    return closed.get();
  }

  protected abstract CursorStream doOpenCursor();
}
