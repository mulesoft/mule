/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.collection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * {@link Collector} that returns a {@link ImmutableList}
 *
 * @param <T> the generic type of the elements in the list
 * @since 4.0
 */
public class ImmutableListCollector<T> implements Collector<T, ImmutableList.Builder<T>, List<T>> {

  @Override
  public Supplier<ImmutableList.Builder<T>> supplier() {
    return ImmutableList::builder;
  }

  @Override
  public BiConsumer<ImmutableList.Builder<T>, T> accumulator() {
    return (builder, value) -> builder.add(value);
  }

  @Override
  public BinaryOperator<ImmutableList.Builder<T>> combiner() {
    return (left, right) -> {
      left.addAll(right.build());
      return left;
    };
  }

  @Override
  public Function<ImmutableList.Builder<T>, List<T>> finisher() {
    return ImmutableList.Builder::build;
  }

  @Override
  public Set<Characteristics> characteristics() {
    return ImmutableSet.of();
  }
}
