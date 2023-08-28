/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util.func;

import static reactor.core.Exceptions.propagate;

/**
 * A {@link Runnable} but for operations which might throw an {@link Exception}
 *
 * @since 4.0
 */
@FunctionalInterface
public interface CheckedRunnable extends Runnable {

  @Override
  default void run() {
    try {
      runChecked();
    } catch (Throwable throwable) {
      handleException(throwable);
    }

  }

  /**
   * Handles the {@code throwable}
   *
   * @param throwable the error that was caught
   */
  default void handleException(Throwable throwable) {
    throw propagate(throwable);
  }

  /**
   * Executes an unsafe operation
   *
   * @throws Exception if anything goes wrong
   */
  void runChecked() throws Exception;
}
