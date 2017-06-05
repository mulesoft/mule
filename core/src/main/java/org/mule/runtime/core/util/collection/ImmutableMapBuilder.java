/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.util.collection;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Builds immutable map instances
 *
 * @param <K> map key's type
 * @param <V> map value's type
 * @since 4.0
 */
public class ImmutableMapBuilder<K, V> {

  private final ImmutableMap.Builder<K, V> builder;

  /**
   * Creates a new builder
   */
  public ImmutableMapBuilder() {
    builder = ImmutableMap.builder();
  }

  /**
   * Associates {@code key} with {@code value} in the built map. Duplicate
   * keys are not allowed, and will cause {@link #build} to fail.
   */
  public ImmutableMapBuilder<K, V> put(K key, V value) {
    builder.put(key, value);
    return this;
  }

  /**
   * Associates all of the given map's keys and values in the built map.
   * Duplicate keys are not allowed, and will cause {@link #build} to fail.
   *
   * @throws NullPointerException if any key or value in {@code map} is null
   */
  public ImmutableMapBuilder<K, V> putAll(Map<? extends K, ? extends V> map) {
    builder.putAll(map);
    return this;
  }

  /**
   * Returns a newly-created immutable {@link Map}.
   *
   * @throws IllegalArgumentException if duplicate keys were added
   */
  public Map<K, V> build() {
    return builder.build();
  }
}
