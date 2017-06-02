/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.util.collection;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class ImmutableMapBuilder<K, V> {

  private final ImmutableMap.Builder<K, V> builder;

  public ImmutableMapBuilder() {
    builder = ImmutableMap.builder();
  }

  public ImmutableMapBuilder<K, V> put(K key, V value) {
    builder.put(key, value);
    return this;
  }

  public ImmutableMapBuilder<K, V> putAll(Map<? extends K, ? extends V> map) {
    builder.putAll(map);
    return this;
  }

  public Map<K, V> build() {
    return builder.build();
  }
}
