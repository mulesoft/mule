/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.mvel;


import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;

import java.util.Iterator;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyIterator;
import static java.util.Collections.singletonList;

public class MVELSplitDataIterator implements Iterator<TypedValue<?>> {

  private Iterator<?> delegate;

  public MVELSplitDataIterator(Iterator<?> delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean hasNext() {
    return delegate.hasNext();
  }

  @Override
  public TypedValue<?> next() {
    Object next = delegate.next();
    return new TypedValue<>(next, DataType.builder().fromObject(next).build());
  }


  public static Iterator<TypedValue<?>> createFrom(Object result) {
    Iterator<Object> iter;
    if (result instanceof Object[]) {
      iter = asList((Object[]) result).iterator();
    } else if (result instanceof Iterable<?>) {
      iter = ((Iterable<Object>) result).iterator();
    } else if (result instanceof Iterator<?>) {
      iter = (Iterator<Object>) result;
    } else if (result instanceof Map<?, ?>) {
      iter = ((Map) result).values().iterator();
    } else if (result == null) {
      iter = emptyIterator();
    } else {
      iter = singletonList(result).iterator();
    }
    return new MVELSplitDataIterator(iter);
  }
}
