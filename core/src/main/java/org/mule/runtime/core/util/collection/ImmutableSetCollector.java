/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.collection;

import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * {@link Collector} that returns a {@link ImmutableSet}
 *
 * @param <T> the generic type of the elements in the {@link Set}
 * @since 4.0
 */
public class ImmutableSetCollector<T> implements Collector<T, ImmutableSetBuilder<T>, Set<T>> {

  @Override
  public Supplier<ImmutableSetBuilder<T>> supplier() {
    return ImmutableSetBuilder::new;
  }

  @Override
  public BiConsumer<ImmutableSetBuilder<T>, T> accumulator() {
    return (builder, value) -> builder.add(value);
  }

  @Override
  public BinaryOperator<ImmutableSetBuilder<T>> combiner() {
    return (left, right) -> {
      left.addAll(right.build());
      return left;
    };
  }

  @Override
  public Function<ImmutableSetBuilder<T>, Set<T>> finisher() {
    return ImmutableSetBuilder::build;
  }

  @Override
  public Set<Characteristics> characteristics() {
    return ImmutableSet.of(Characteristics.UNORDERED);
  }
}
