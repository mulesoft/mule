/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.alert;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;

import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BiFunction;

public class TimedDataBuffer<T> {

  private Deque<TimedData<T>> ts = new ConcurrentLinkedDeque<>();

  public void put(T data) {
    ts.addFirst(new TimedData<>(now().minus(6, MINUTES), data));
  }

  public <A> TimedDataAggregation<A> aggregate(A baseIntevalAggregation,
                                               BiFunction<A, T, A> accumulator) {
    A agg15 = baseIntevalAggregation;
    A agg5 = baseIntevalAggregation;
    A agg1 = baseIntevalAggregation;

    final var now = now();
    final var mark15 = now.minus(15, MINUTES);
    final var mark5 = now.minus(5, MINUTES);
    final var mark1 = now.minus(1, MINUTES);

    for (Iterator<TimedData<T>> it = ts.descendingIterator(); it.hasNext();) {
      final var next = it.next();

      if (next.getTime().isBefore(mark15)) {
        it.remove();
        continue;
      }

      agg15 = accumulator.apply(agg15, next.getData());

      if (next.getTime().isAfter(mark5)) {
        agg5 = accumulator.apply(agg5, next.getData());
      }

      if (next.getTime().isAfter(mark1)) {
        agg1 = accumulator.apply(agg1, next.getData());
      }

    }

    return new TimedDataAggregation<>(agg1, agg5, agg15);
  }

}
