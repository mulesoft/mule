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

/**
 * Builds immutable set instances
 *
 * @param <T> set element's type
 * @since 4.0
 */
public class ImmutableSetBuilder<T> {

  private final ImmutableSet.Builder<T> builder;

  /**
   * Returns a new builder.
   */
  public ImmutableSetBuilder() {
    builder = ImmutableSet.builder();
  }

  /**
   * Adds {@code element} to the {@code ImmutableSet}.  If the {@code
   * ImmutableSet} already contains {@code element}, then {@code add} has no
   * effect (only the previously added element is retained).
   *
   * @param element the element to add
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code element} is null
   */
  public void add(T element) {
    builder.add(element);
  }

  /**
   * Adds each element of {@code elements} to the {@code ImmutableSet},
   * ignoring duplicate elements (only the first duplicate element is added).
   *
   * @param elements the {@code Iterable} to add to the {@code ImmutableSet}
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code elements} is null or contains a
   *     null element
   */
  public ImmutableSetBuilder<T> addAll(Iterable<? extends T> elements) {
    builder.addAll(elements);
    return this;
  }

  /**
   * Adds each element of {@code elements} to the {@code ImmutableSet},
   * ignoring duplicate elements (only the first duplicate element is added).
   *
   * @param elements the elements to add to the {@code ImmutableSet}
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code elements} is null or contains a
   *     null element
   */
  public ImmutableSetBuilder<T> addAll(Iterator<? extends T> elements) {
    builder.addAll(elements);
    return this;
  }

  /**
   * Returns a newly-created immutable {@link Set} based on the added elements
   */
  public Set<T> build() {
    return builder.build();
  }
}
