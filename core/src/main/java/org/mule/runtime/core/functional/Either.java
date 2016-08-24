/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.functional;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

final public class Either<L, R> {

  public static <L, R> Either<L, R> left(L value) {
    return new Either<>(Optional.of(value), Optional.empty());
  }

  public static <L, R> Either<L, R> right(R value) {
    return new Either<>(Optional.empty(), Optional.of(value));
  }

  private final Optional<L> left;
  private final Optional<R> right;

  private Either(Optional<L> l, Optional<R> r) {
    left = l;
    right = r;
  }

  public <T> Either<T, R> mapLeft(Function<? super L, ? extends T> lFunc) {
    return new Either<>(left.map(lFunc), right);
  }

  public <T> Either<L, T> mapRight(Function<? super R, ? extends T> rFunc) {
    return new Either<>(left, right.map(rFunc));
  }

  public void apply(Consumer<? super L> lFunc, Consumer<? super R> rFunc) {
    left.ifPresent(lFunc);
    right.ifPresent(rFunc);
  }

  public boolean isLeft() {
    return left.isPresent();
  }

  public boolean isRight() {
    return right.isPresent();
  }

  public L getLeft() {
    return left.get();
  }

  public R getRight() {
    return right.get();
  }
}
