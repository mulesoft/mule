/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util.func;

import static org.mule.runtime.core.api.rx.Exceptions.propagateWrappingFatal;

import java.util.function.Predicate;

@FunctionalInterface
public interface CheckedPredicate<T> extends Predicate<T> {

  @Override
  default boolean test(T t) {
    try {
      return testChecked(t);
    } catch (Throwable throwable) {
      throw propagateWrappingFatal(throwable);
    }
  }

  boolean testChecked(T t) throws Throwable;
}
