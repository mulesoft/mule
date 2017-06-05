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
// TODO(pablo.kraan): MULE-12608 - Add a ImmutableCollectors utility class to replace all the ImmutableXCollector classes.
public class ImmutableListCollector<T> implements Collector<T, ImmutableListBuilder<T>, List<T>> {

  @Override
  public Supplier<ImmutableListBuilder<T>> supplier() {
    return ImmutableListBuilder::new;
  }

  @Override
  public BiConsumer<ImmutableListBuilder<T>, T> accumulator() {
    return (builder, value) -> builder.add(value);
  }

  @Override
  public BinaryOperator<ImmutableListBuilder<T>> combiner() {
    return (left, right) -> {
      left.addAll(right.build());
      return left;
    };
  }

  @Override
  public Function<ImmutableListBuilder<T>, List<T>> finisher() {
    return ImmutableListBuilder::build;
  }

  @Override
  public Set<Characteristics> characteristics() {
    return ImmutableSet.of();
  }
}
