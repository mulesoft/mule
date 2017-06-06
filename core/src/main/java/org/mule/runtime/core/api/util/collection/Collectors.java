/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.util.collection;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;

/**
 * Provides different implementations of {@link Collector}
 *
 * @since 4.0
 */
public class Collectors {

  private Collectors() {}

  /**
   * Returns a {@code Collector} that accumulates the input elements into an immutable {@code List}.
   *
   * @param <T> the type of the input elements
   * @return a {@code Collector} which collects all the input elements into an immutable {@code List}, in encounter order
   */
  public static <T> Collector<T, ?, List<T>> toImmutableList() {
    return new ImmutableListCollector<>();
  }

  /**
   * Returns a {@code Collector} that accumulates the input elements into an immutable {@code Set}.
   *
   * @param <T> the type of the input elements
   * @return a {@code Collector} which collects all the input elements into an immutable {@code Set}, in encounter order
   */
  public static <T> Collector<T, ?, Set<T>> toImmutableSet() {
    return new ImmutableSetCollector<>();
  }

  /**
   * Returns a {@code Collector} that accumulates elements into an immutable {@code Map} whose keys and values are the result of
   * applying the provided mapping functions to the input elements.
   *
   * @param <T> the type of the input elements
   * @param <K> the output type of the key mapping function
   * @param <U> the output type of the value mapping function
   * @param keyMapper a mapping function to produce keys
   * @param valueMapper a mapping function to produce values
   * @return a {@code Collector} which collects elements into a {@code Map}
   * whose keys and values are the result of applying mapping functions to
   * the input elements
   */
  public static <T, K, V> Collector<T, ?, Map<K, V>> toImmutableMap(Function<T, K> keyMapper, Function<T, V> valueMapper) {
    return new ImmutableMapCollector<>(keyMapper, valueMapper);
  }
}
