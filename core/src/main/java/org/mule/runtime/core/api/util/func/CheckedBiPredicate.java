/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util.func;

import static org.mule.runtime.core.api.rx.Exceptions.propagateWrappingFatal;

import java.util.function.BiPredicate;

@FunctionalInterface
public interface CheckedBiPredicate<T, U> extends BiPredicate<T, U> {

  @Override
  default boolean test(T t, U u) {
    try {
      return testChecked(t, u);
    } catch (Throwable throwable) {
      throw propagateWrappingFatal(throwable);
    }
  }

  boolean testChecked(T t, U u) throws Throwable;
}
