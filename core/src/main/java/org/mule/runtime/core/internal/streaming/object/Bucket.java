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
 * 
 * @param <T>
 */
public class Bucket<T> {

  private final List<T> items;
  private final int capacity;
  private final int index;

  public Bucket(int index, int capacity) {
    this.index = index;
    this.capacity = capacity;
    this.items = new ArrayList<>(capacity);
  }

  public Optional<T> get(int index) {
    if (index < items.size()) {
      return ofNullable(items.get(index));
    }
    return empty();
  }

  public boolean contains(Position position) {
    return index == position.getBucketIndex() && position.getItemIndex() < items.size();
  }

  public int getIndex() {
    return index;
  }

  public boolean add(T item) {
    if (items.size() < capacity) {
      items.add(item);
      return true;
    }

    return false;
  }
}
