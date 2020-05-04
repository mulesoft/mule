/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.mediatype;

import org.mule.runtime.api.streaming.HasSize;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * {@code Iterator<Result>} that decorates each of its delegate elements using a {@link PayloadMediaTypeResolver}
 *
 * This allows to avoid preemptive decoration of an entire collection of {@link Result}
 *
 * @since 4.2
 */
public class MediaTypeDecoratedResultIterator implements Iterator<Result>, HasSize {

  private final Iterator<Result> delegate;
  private final PayloadMediaTypeResolver payloadMediaTypeResolver;

  public MediaTypeDecoratedResultIterator(Iterator<Result> delegate, PayloadMediaTypeResolver payloadMediaTypeResolver) {
    this.delegate = delegate;
    this.payloadMediaTypeResolver = payloadMediaTypeResolver;
  }

  @Override
  public boolean hasNext() {
    return delegate.hasNext();
  }

  @Override
  public Result next() {
    return payloadMediaTypeResolver.resolve(delegate.next());
  }

  @Override
  public int getSize() {
    return delegate instanceof HasSize ? ((HasSize) delegate).getSize() : -1;
  }

  @Override
  public void remove() {
    delegate.remove();
  }

  @Override
  public void forEachRemaining(Consumer<? super Result> action) {
    delegate.forEachRemaining(value -> {
      action.accept(payloadMediaTypeResolver.resolve(value));
    });
  }
}
