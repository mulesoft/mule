/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.error;

import org.mule.runtime.api.message.Error;

/**
 * Defines a metric oriented interface for {@link Error}.
 *
 * @since 4.9.0
 */
public interface ErrorMetrics {

  /**
   * No operation {@link ErrorMetrics} implementation.
   */
  ErrorMetrics NO_OP = new ErrorMetrics() {

    @Override
    public void measure(Error value) {
      // Nothing to do
    }

    @Override
    public void measure(Throwable value) {
      // Nothing to do
    }
  };

  /**
   * Measures an {@link Error}.
   *
   * @param value An {@link Error} to be measured.
   */
  void measure(Error value);

  /**
   * Measures a {@link Throwable}. Strictly avoid calling this method when {@link #measure(Error)} is an option.
   *
   * @param value An {@link Error} to be measured.
   */
  void measure(Throwable value);
}
