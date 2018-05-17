/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.mediatype;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * @since 4.2
 */
public class MediaTypeDecoratedResultList extends MediaTypeDecoratedResultCollection implements List<Result> {

  private List<Result> delegate;

  public MediaTypeDecoratedResultList(List<Result> delegate, MediaTypeResolver mediaTypeResolver) {
    super(delegate, mediaTypeResolver);
    this.delegate = delegate;
  }

  @Override
  public boolean addAll(int index, Collection<? extends Result> c) {
    return delegate.addAll(index, c);
  }

  @Override
  public Result get(int index) {
    return mediaTypeResolver.resolve(delegate.get(index));
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
    int indexA = delegate.indexOf(o);
    int indexB = -1;
    if (o instanceof Result) {
      indexB = delegate.indexOf(mediaTypeResolver.resolve((Result) o));
    }
    if (indexA > -1 && indexB > -1) {
      return Math.min(indexA, indexB);
    } else if (indexA > -1) {
      return indexA;
    } else {
      return indexB;
    }
  }

  @Override
  public int lastIndexOf(Object o) {
    int indexA = delegate.lastIndexOf(o);
    int indexB = -1;
    if (o instanceof Result) {
      indexB = delegate.lastIndexOf(mediaTypeResolver.resolve((Result) o));
    }
    if (indexA > -1 && indexB > -1) {
      return Math.max(indexA, indexB);
    } else if (indexA > -1) {
      return indexA;
    } else {
      return indexB;
    }
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
    return new MediaTypeDecoratedResultList(delegate.subList(fromIndex, toIndex), mediaTypeResolver);
  }
}
