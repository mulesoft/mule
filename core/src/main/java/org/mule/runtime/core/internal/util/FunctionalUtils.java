/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import org.mule.runtime.core.api.util.func.CheckedRunnable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utilities for functional programming
 *
 * @since 4.0
 */
public class FunctionalUtils {

  /**
   * Executes the given task, ignoring thrown exceptions. <b>Use with care</b>
   *
   * @param task the task to execute
   */
  public static void safely(CheckedRunnable task) {
    safely(task, e -> {
    });
  }

  /**
   * Executes the given {@code task}, feeding any thrown exceptions into the given
   * {@code exceptionHandler}
   *
   * @param task             a task
   * @param exceptionHandler an exception {@link Consumer}
   */
  public static void safely(CheckedRunnable task, Consumer<Exception> exceptionHandler) {
    try {
      task.run();
    } catch (Exception e) {
      exceptionHandler.accept(e);
    }
  }

  /**
   * If the given {@code optional} is not present, then it returns the optional
   * provided by the {@code orElse} supplier
   *
   * @param optional an {@link Optional}
   * @param orElse   a {@link Supplier} that provides the return value in case the {@code optional} is empty
   * @param <T>      the generic type of the optional item
   * @return an {@link Optional}
   */
  public static <T> Optional<T> or(Optional<T> optional, Supplier<Optional<T>> orElse) {
    return optional.isPresent() ? optional : orElse.get();
  }

  /**
   * If the {@code optional} is present, it executes the {@code ifPresent} consumer. Otherwise,
   * it executes the {@code orElse} runnable
   *
   * @param optional  an {@link Optional} value
   * @param ifPresent the consumer to execute if the value is present
   * @param orElse    a fallback runnable in case the optional is empty.
   * @param <T>       the generic type of the optional's value.
   */
  public static <T> void ifPresent(Optional<T> optional, Consumer<? super T> ifPresent, Runnable orElse) {
    if (optional.isPresent()) {
      ifPresent.accept(optional.get());
    } else {
      orElse.run();
    }
  }
}
