/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.func;

/**
 * Similar to {@link Runnable} but for operations which might throw
 * a {@link Exception}
 *
 * @since 4.0
 */
@FunctionalInterface
public interface CheckedRunnable {

  /**
   * Executes an unsafe operation
   *
   * @throws Exception if anything goes wrong
   */
  void run() throws Exception;
}
