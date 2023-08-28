/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util.func;

import static org.mule.runtime.core.api.rx.Exceptions.propagateWrappingFatal;

import java.util.function.Consumer;

@FunctionalInterface
public interface CheckedConsumer<T> extends Consumer<T> {

  @Override
  default void accept(T t) {
    try {
      acceptChecked(t);
    } catch (Throwable throwable) {
      throw propagateWrappingFatal(throwable);
    }
  }

  void acceptChecked(T t) throws Throwable;
}
