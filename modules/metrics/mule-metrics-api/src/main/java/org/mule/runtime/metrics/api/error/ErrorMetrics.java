/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.error;

import org.mule.runtime.api.message.Error;

public interface ErrorMetrics {

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

  void measure(Error value);

  void measure(Throwable value);
}
