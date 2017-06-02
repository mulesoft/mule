/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.util.collection;

import com.google.common.collect.ImmutableSet;

import java.util.Iterator;
import java.util.Set;

public class ImmutableSetBuilder<T> {

  private final ImmutableSet.Builder<T> builder;

  public ImmutableSetBuilder() {
    builder = ImmutableSet.builder();
  }

  public void add(T value) {
    builder.add(value);
  }

  public ImmutableSetBuilder<T> addAll(Iterable<? extends T> elements) {
    builder.addAll(elements);
    return this;
  }

  public ImmutableSetBuilder<T> addAll(Iterator<? extends T> elements) {
    builder.addAll(elements);
    return this;
  }

  public Set<T> build() {
    return builder.build();
  }
}
