/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.functional;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This class allow to represent a type that holds two different values.
 *
 * Only one value can be present at any given type.
 *
 * This class can be used as a monad to interact and chain functions to be executed over the possible return values.
 *
 * Most likely the left type represent an error or failure result and the right value represent a successful result.
 *
 * @param <L> the type of the left value
 * @param <R> the type of the right value
 */
final public class Either<L, R> {

  /**
   * Creates an {@code Either} with a left value.
   *
   * @param value the left value
   * @param <L> the left value type
   * @param <R> the right value type
   * @return the created {@code Either instance}
   */
  public static <L, R> Either<L, R> left(L value) {
    return new Either<>(Optional.of(value), Optional.empty());
  }

  /**
   * Creates an {@code Either} with a right value.
   *
   * @param value the right value
   * @param <L> the left value type
   * @param <R> the right value type
   * @return the created {@code Either instance}
   */
  public static <L, R> Either<L, R> right(R value) {
    return new Either<>(Optional.empty(), Optional.of(value));
  }

  private final Optional<L> left;
  private final Optional<R> right;

  private Either(Optional<L> l, Optional<R> r) {
    left = l;
    right = r;
  }

  /**
   * Allows to execute a function over the left value if it is present
   * 
   * @param lFunc the function to apply to the left value
   * @param <T> the return type of the function.
   * @return a new {@code Either} created from the result of applying the function.
   */
  public <T> Either<T, R> mapLeft(Function<? super L, ? extends T> lFunc) {
    return new Either<>(left.map(lFunc), right);
  }

  /**
   * Allows to execute a function over the right value if it is present
   * 
   * @param rFunc the function to apply to the right value
   * @param <T> the return type of the function.
   * @return a new {@code Either} created from the result of applying the function.
   */
  public <T> Either<L, T> mapRight(Function<? super R, ? extends T> rFunc) {
    return new Either<>(left, right.map(rFunc));
  }

  /**
   * Receives a {@link Consumer} functions for both, the left and right value and applies the one over the value that is present.
   *
   * @param lFunc the function to apply to the left value
   * @param rFunc the function to apply to the right value
   */
  public void apply(Consumer<? super L> lFunc, Consumer<? super R> rFunc) {
    left.ifPresent(lFunc);
    right.ifPresent(rFunc);
  }

  /**
   * @return true if it holds a value for the left type, false otherwise
   */
  public boolean isLeft() {
    return left.isPresent();
  }

  /**
   * @return true if it holds a value for the right type, false otherwise
   */
  public boolean isRight() {
    return right.isPresent();
  }

  /**
   * @return the left value
   */
  public L getLeft() {
    return left.get();
  }

  /**
   * @return the right value
   */
  public R getRight() {
    return right.get();
  }
}
