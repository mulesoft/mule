/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import static org.mule.runtime.api.util.Preconditions.checkState;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.streaming.iterator.StreamingIterator;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for {@link CursorIteratorProvider} implementations.
 *
 * @since 4.0
 */
public abstract class AbstractCursorIteratorProvider extends AbstractComponent implements CursorIteratorProvider {

  protected final StreamingIterator stream;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new instance
   *
   * @param stream the original stream to be decorated
   */
  public AbstractCursorIteratorProvider(StreamingIterator<?> stream) {
    this.stream = stream;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final CursorIterator openCursor() {
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

  protected abstract CursorIterator doOpenCursor();
}
