/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.collection;

import java.util.Map;

/**
 * Immutable {@link Map.Entry} implementation.
 *
 * @param <K> the type of the key
 * @param <V> the type of the value
 */
public class ImmutableEntry<K, V> implements Map.Entry<K, V> {

  private final Map.Entry<K, V> entry;

  public ImmutableEntry(Map.Entry<K, V> entry) {
    this.entry = entry;
  }

  @Override
  public K getKey() {
    return entry.getKey();
  }

  @Override
  public V getValue() {
    return entry.getValue();
  }

  @Override
  public V setValue(V value) {
    throw new UnsupportedOperationException("It's not possible to update a map entry result of a map iteration");
  }
}
