/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util.func;

import static org.mule.runtime.core.api.rx.Exceptions.propagateWrappingFatal;

import java.util.function.BiFunction;

@FunctionalInterface
public interface CheckedBiFunction<T, U, R> extends BiFunction<T, U, R> {

  @Override
  default R apply(T t, U u) {
    try {
      return applyChecked(t, u);
    } catch (Throwable throwable) {
      throw propagateWrappingFatal(throwable);
    }
  }

  R applyChecked(T t, U u) throws Throwable;
}
