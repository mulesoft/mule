/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
   * @param throwable the error that was catched
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
