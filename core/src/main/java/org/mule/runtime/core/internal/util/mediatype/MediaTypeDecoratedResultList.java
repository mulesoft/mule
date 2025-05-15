/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.mediatype;


import org.mule.sdk.api.runtime.operation.Result;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * {@code List<Result>} that decorates each of its delegate elements using a {@link PayloadMediaTypeResolver}
 *
 * This allows to avoid preemptive decoration of an entire collection of {@link Result}
 *
 * @since 4.2
 */
public class MediaTypeDecoratedResultList extends MediaTypeDecoratedResultCollection implements List<Result> {

  private List<Result> delegate;

  public MediaTypeDecoratedResultList(List<Result> delegate, PayloadMediaTypeResolver payloadMediaTypeResolver) {
    super(delegate, payloadMediaTypeResolver);
    this.delegate = delegate;
  }

  @Override
  public boolean addAll(int index, Collection<? extends Result> c) {
    return delegate.addAll(index, c);
  }

  @Override
  public Result get(int index) {
    return payloadMediaTypeResolver.resolve(delegate.get(index));
  }

  @Override
  public Result set(int index, Result element) {
    return delegate.set(index, element);
  }

  @Override
  public void add(int index, Result element) {
    delegate.add(index, element);
  }

  @Override
  public Result remove(int index) {
    return delegate.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    if (o instanceof Result) {
      return delegate.indexOf(payloadMediaTypeResolver.resolve((Result) o));
    }
    return delegate.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    if (o instanceof Result) {
      return delegate.lastIndexOf(payloadMediaTypeResolver.resolve((Result) o));
    }
    return delegate.lastIndexOf(o);
  }

  @Override
  public ListIterator<Result> listIterator() {
    return new MediaTypeDecoratedResultListIterator(this);
  }

  @Override
  public ListIterator<Result> listIterator(int index) {
    return new MediaTypeDecoratedResultListIterator(this, index);
  }

  @Override
  public List<Result> subList(int fromIndex, int toIndex) {
    return new MediaTypeDecoratedResultList(delegate.subList(fromIndex, toIndex), payloadMediaTypeResolver);
  }
}
