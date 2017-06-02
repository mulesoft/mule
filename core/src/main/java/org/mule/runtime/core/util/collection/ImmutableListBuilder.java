/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.util.collection;

import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.List;

public class ImmutableListBuilder<T> {

  private final ImmutableList.Builder<T> builder;

  public ImmutableListBuilder() {
    builder = ImmutableList.builder();
  }

  public void add(T value) {
    builder.add(value);
  }

  public ImmutableListBuilder<T> addAll(Iterable<? extends T> elements) {
    builder.addAll(elements);
    return this;
  }

  public ImmutableListBuilder<T> addAll(Iterator<? extends T> elements) {
    builder.addAll(elements);
    return this;
  }

  public List<T> build() {
    return builder.build();
  }
}
