/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util;

import org.mule.runtime.core.util.func.CheckedRunnable;

import java.util.function.Consumer;

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
}
