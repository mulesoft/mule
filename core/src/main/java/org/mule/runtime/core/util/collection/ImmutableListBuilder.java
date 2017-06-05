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

/**
 * Builds immutable list instances
 *
 * @param <T> list element's type
 * @since 4.0
 */
public class ImmutableListBuilder<T> {

  private final ImmutableList.Builder<T> builder;

  /**
   * Creates a new builder.
   */
  public ImmutableListBuilder() {
    builder = ImmutableList.builder();
  }

  /**
   * Adds {@code element} to the {@code ImmutableList}.
   *
   * @param element the element to add
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code element} is null
   */
  public void add(T element) {
    builder.add(element);
  }

  public ImmutableListBuilder<T> addAll(Iterable<? extends T> elements) {
    builder.addAll(elements);
    return this;
  }

  /**
   * Adds each element of {@code elements} to the {@code ImmutableList}.
   *
   * @param elements the {@code Iterable} to add to the {@code ImmutableList}
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code elements} is null or contains a
   *     null element
   */
  public ImmutableListBuilder<T> addAll(Iterator<? extends T> elements) {
    builder.addAll(elements);
    return this;
  }

  /**
   * Returns a newly-created immutable {@link List} based on the added elements
   */
  public List<T> build() {
    return builder.build();
  }
}
