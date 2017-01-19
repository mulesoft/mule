/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static org.mule.runtime.api.util.Preconditions.checkState;
import org.mule.runtime.api.streaming.CursorStream;
import org.mule.runtime.core.api.Event;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractCursorStreamProviderAdapter implements CursorStreamProviderAdapter {

  protected final InputStream wrappedStream;

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final Event creatorEvent;

  /**
   * Creates a new instance
   *
   * @param wrappedStream the original stream to be decorated
   */
  public AbstractCursorStreamProviderAdapter(InputStream wrappedStream, Event event) {
    this.wrappedStream = wrappedStream;
    this.creatorEvent = event;
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Event getCreatorEvent() {
    return creatorEvent;
  }

  protected abstract CursorStream doOpenCursor();
}
