/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util.func;

import static org.mule.runtime.core.api.rx.Exceptions.propagateWrappingFatal;

import java.util.function.Function;

/**
 * A {@link Function} which can throw exceptions
 *
 * @param <T> the generic type of the return value
 * @since 4.0
 */
@FunctionalInterface
public interface CheckedFunction<T, R> extends Function<T, R> {

  @Override
  default R apply(T t) {
    try {
      return applyChecked(t);
    } catch (Throwable throwable) {
      return handleException(throwable);
    }
  }

  /**
   * Handles the {@code throwable}
   *
   * @param throwable the error that was caught
   */
  default R handleException(Throwable throwable) {
    throw propagateWrappingFatal(throwable);
  }

  /**
   * Executes an unsafe operation
   *
   * @throws Exception if anything goes wrong
   */
  R applyChecked(T t) throws Throwable;
}
