/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.internal.event.NullEventFactory;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.function.Consumer;

/**
 * Utilities for functional programming
 *
 * @since 4.0
 */
public class FunctionalUtils extends org.mule.runtime.api.util.FunctionalUtils {

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
   * Executes the given {@code task}, feeding any thrown exceptions into the given {@code exceptionHandler}
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
   * Executes the given {@code task}, feeding any thrown exceptions into the given {@code exceptionHandler}
   *
   * @param item             the object to perform the task on
   * @param task             a task
   * @param exceptionHandler an exception {@link Consumer}
   */
  public static <T> void safely(T item, CheckedConsumer<T> task, Consumer<Exception> exceptionHandler) {
    try {
      task.accept(item);
    } catch (Exception e) {
      exceptionHandler.accept(e);
    }
  }

  /**
   * Executes the given {@code function} using an event obtained through {@link NullEventFactory#getNullEvent()}.
   * <p>
   * The event context is automatically completed (either with success or error) depending on the function providing a result or
   * throwing an exception.
   *
   * @param function the function to execute
   * @param <T>      the generic type of the functions return value
   * @return the function's value
   * @throws RuntimeException if the function itself throws one.
   * @since 4.5.0
   */
  public static <T> T withNullEvent(CheckedFunction<CoreEvent, T> function) {
    CoreEvent event = getNullEvent();
    try {
      T value = function.apply(event);
      ((BaseEventContext) event.getContext()).success();
      return value;
    } catch (RuntimeException e) {
      ((BaseEventContext) event.getContext()).error(e);
      throw e;
    }
  }
}
