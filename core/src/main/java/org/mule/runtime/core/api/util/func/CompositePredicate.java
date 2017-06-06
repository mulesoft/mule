/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.func;

import static com.google.common.collect.ImmutableList.copyOf;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.function.Predicate;

/**
 * A {@link Predicate} which aggregates more predicates and evaluates them
 * as a whole.
 *
 * @param <T> the generic type of the predicate
 * @since 4.0
 */
public class CompositePredicate<T> implements Predicate<T> {

  private final List<Predicate<T>> predicates;

  /**
   * Creates a new instance which aggregates the given {@code predicates}.
   * If {@code predicates} is {@code null} or empty, then the returned
   * instance will return {@code true} for any value
   *
   * @param predicates the predicates to aggregate
   * @param <T>        the generic type of the predicate
   * @return a new {@link CompositePredicate}
   */
  public static <T> CompositePredicate<T> of(Predicate<T>... predicates) {
    return new CompositePredicate<>(predicates == null
        ? ImmutableList.of()
        : copyOf(predicates));
  }

  private CompositePredicate(List<Predicate<T>> predicates) {
    this.predicates = predicates;
  }

  /**
   * Tests the value with all the aggregated predicates.
   * <p>
   * The predicates will be evaluated in the same order as they were
   * fed into the {@link #of(Predicate[])} method. If one predicate
   * returns {@code false}, then the subsequent ones will not be evaluated
   * and this method will return {@code false}.
   *
   * @param t the value to test
   * @return the result of the evaluation
   */
  @Override
  public boolean test(T t) {
    for (Predicate<T> predicate : predicates) {
      if (!predicate.test(t)) {
        return false;
      }
    }

    return true;
  }
}
