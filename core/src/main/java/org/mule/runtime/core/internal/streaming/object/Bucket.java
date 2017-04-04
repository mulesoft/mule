/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A zero-based indexed group of items.
 * <p>
 * Buckets have a fixed capacity. Once reached, the bucket will accept no more items
 *
 * @param <T> the generic type of the items
 * @since 4.0
 */
public class Bucket<T> {

  private final List<T> items;
  private final int capacity;
  private final int index;

  /**
   * Creates a new instance
   *
   * @param index    the bucket's index.
   * @param capacity the bucket's capacity.
   */
  public Bucket(int index, int capacity) {
    this.index = index;
    this.capacity = capacity;
    this.items = new ArrayList<>(capacity);
  }

  /**
   * Obtains the value that was added at the given {@code index} through the {@link #add(Object)} method.
   * If no such value was added, it will return an empty value.
   *
   * @param index the item's index
   * @return an {@link Optional} value
   */
  public Optional<T> get(int index) {
    if (index < items.size()) {
      return ofNullable(items.get(index));
    }
    return empty();
  }

  /**
   * @param position a {@link Position}
   * @return Whether this bucket contains an item for the given {@code position}
   */
  public boolean contains(Position position) {
    return index == position.getBucketIndex() && position.getItemIndex() < items.size();
  }

  /**
   * @return {@code this} bucket's index
   */
  public int getIndex() {
    return index;
  }

  /**
   * Adds the given {@code item} if the bucket still has capacity.
   *
   * @param item the item to be added
   * @return whether the item was accepted or not
   */
  public boolean add(T item) {
    if (items.size() < capacity) {
      items.add(item);
      return true;
    }

    return false;
  }
}
