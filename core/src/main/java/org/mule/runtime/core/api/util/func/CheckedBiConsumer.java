/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util.func;

import static org.mule.runtime.core.api.rx.Exceptions.propagateWrappingFatal;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface CheckedBiConsumer<T, U> extends BiConsumer<T, U> {

  @Override
  default void accept(T t, U u) {
    try {
      acceptChecked(t, u);
    } catch (Throwable throwable) {
      throw propagateWrappingFatal(throwable);
    }
  }

  void acceptChecked(T t, U u) throws Throwable;
}
