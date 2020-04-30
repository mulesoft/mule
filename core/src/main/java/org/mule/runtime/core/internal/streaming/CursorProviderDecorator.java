/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;

import java.util.Optional;

public abstract class CursorProviderDecorator<T extends Cursor> implements CursorProvider<T> {

  protected final CursorProvider<T> delegate;

  public CursorProviderDecorator(CursorProvider<T> delegate) {
    this.delegate = delegate;
  }

  public CursorProvider<T> getDelegate() {
    return delegate;
  }

  @Override
  public T openCursor() {
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
  public Optional<ComponentLocation> getOriginatingLocation() {
    return delegate.getOriginatingLocation();
  }

  @Override
  public boolean equals(Object o) {
    return delegate.equals(o);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }
}
