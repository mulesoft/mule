/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.mediatype;

import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * @since 4.2
 */
public class MediaTypeDecoratedResultIterator implements Iterator<Result> {

  private Iterator<Result> delegate;
  private MediaTypeResolver mediaTypeResolver;

  public MediaTypeDecoratedResultIterator(Iterator<Result> delegate, MediaTypeResolver mediaTypeResolver) {
    this.delegate = delegate;
    this.mediaTypeResolver = mediaTypeResolver;
  }

  @Override
  public boolean hasNext() {
    return delegate.hasNext();
  }

  @Override
  public Result next() {
    return mediaTypeResolver.resolve(delegate.next());
  }

  @Override
  public void remove() {
    delegate.remove();
  }

  @Override
  public void forEachRemaining(Consumer<? super Result> action) {
    delegate.forEachRemaining(value -> {
      action.accept(mediaTypeResolver.resolve(value));
    });
  }
}
