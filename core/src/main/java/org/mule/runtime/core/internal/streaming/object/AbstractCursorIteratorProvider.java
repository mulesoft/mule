/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import static java.util.Optional.ofNullable;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.internal.streaming.CursorProviderAlreadyClosedException;

/**
 * Base class for {@link CursorIteratorProvider} implementations.
 *
 * @since 4.0
 */
public abstract class AbstractCursorIteratorProvider implements CursorIteratorProvider {

  protected final Iterator stream;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final ComponentLocation originatingLocation;
  private Exception closerResponsible;

  private final boolean trackCursorProviderClose;

  /**
   * Creates a new instance
   *
   * @param stream the original stream to be decorated
   * @param originatingLocation indicates where the provider was created
   */
  public AbstractCursorIteratorProvider(Iterator<?> stream, ComponentLocation originatingLocation,
                                        boolean trackCursorProviderClose) {
    this.stream = stream;
    this.originatingLocation = originatingLocation;
    this.trackCursorProviderClose = trackCursorProviderClose;
  }

  /**
   * Creates a new instance
   *
   * @param stream the original stream to be decorated
   * @deprecated Please use {@link #AbstractCursorIteratorProvider(Iterator, ComponentLocation, boolean)} instead.
   */
  @Deprecated
  public AbstractCursorIteratorProvider(Iterator<?> stream) {
    this(stream, null, false);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public final CursorIterator openCursor() {
    if (closed.get()) {
      throw new CursorProviderAlreadyClosedException("Cannot open a new cursor on a closed iterator",
                                                     getOriginatingLocation(),
                                                     ofNullable(closerResponsible));
    }
    return doOpenCursor();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    closed.set(true);
    if (trackCursorProviderClose) {
      closerResponsible = new Exception("Responsible for closing the stream.");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isClosed() {
    return closed.get();
  }

  protected abstract CursorIterator doOpenCursor();

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ComponentLocation> getOriginatingLocation() {
    return ofNullable(originatingLocation);
  }
}
