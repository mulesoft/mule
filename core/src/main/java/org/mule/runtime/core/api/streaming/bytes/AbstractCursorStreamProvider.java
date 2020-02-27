/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming.bytes;

import static java.lang.Boolean.getBoolean;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.util.MuleSystemProperties.TRACK_CURSOR_PROVIDER_CLOSE_PROPERTY;

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.internal.streaming.CursorProviderAlreadyClosedException;

/**
 * Base class for {@link CursorStreamProvider} implementations.
 *
 * @since 4.0
 */
@NoExtend
public abstract class AbstractCursorStreamProvider extends AbstractComponent implements CursorStreamProvider {

  protected final InputStream wrappedStream;

  private final AtomicBoolean closed = new AtomicBoolean(false);

  private final ComponentLocation originatingLocation;

  private Exception closerResponsible;

  private static final boolean TRACK_CURSOR_PROVIDER_CLOSE = getBoolean(TRACK_CURSOR_PROVIDER_CLOSE_PROPERTY);

  /**
   * Creates a new instance
   *
   * @param wrappedStream the original stream to be decorated
   * @param originatingLocation indicates where the provider was created
   */
  public AbstractCursorStreamProvider(InputStream wrappedStream, ComponentLocation originatingLocation) {
    this.wrappedStream = wrappedStream;
    this.originatingLocation = originatingLocation;
  }

  /**
   * Creates a new instance
   *
   * @param wrappedStream the original stream to be decorated
   *
   * @deprecated Please use {@link #AbstractCursorStreamProvider(InputStream, ComponentLocation)} instead.
   */
  @Deprecated
  public AbstractCursorStreamProvider(InputStream wrappedStream) {
    this(wrappedStream, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final CursorStream openCursor() {
    if (closed.get()) {
      throw new CursorProviderAlreadyClosedException("Cannot open a new cursor on a closed stream",
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
    if (TRACK_CURSOR_PROVIDER_CLOSE) {
      closerResponsible = new Exception("Responsible for closing the stream.");
    }
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ComponentLocation> getOriginatingLocation() {
    return ofNullable(originatingLocation);
  }
}
