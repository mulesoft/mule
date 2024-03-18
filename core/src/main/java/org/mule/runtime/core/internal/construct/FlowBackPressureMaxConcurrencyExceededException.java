/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import org.mule.runtime.api.component.Component;

import java.util.concurrent.RejectedExecutionException;

import static org.mule.runtime.core.api.construct.BackPressureReason.MAX_CONCURRENCY_EXCEEDED;

/**
 * The flow is already processing the number of events required by its maxConcurrency.
 *
 * @since 4.3, 4.2.2
 */
public class FlowBackPressureMaxConcurrencyExceededException extends FlowBackPressureException {

  private static final long serialVersionUID = 6226082055699845061L;

  /**
   * Create a new {@link FlowBackPressureMaxConcurrencyExceededException} with no cause. This is typically use when a stream based
   * processing exerts back-pressure without throwing an exception.
   */
  public FlowBackPressureMaxConcurrencyExceededException(Component flow) {
    super(flow, MAX_CONCURRENCY_EXCEEDED);
  }

  /**
   * Create a new {@link FlowBackPressureMaxConcurrencyExceededException} with a cause. This is typically use when a non-stream
   * based processing strategy is in use and back-pressure is identified by a {@link RejectedExecutionException}.
   */
  public FlowBackPressureMaxConcurrencyExceededException(Component flow, Throwable cause) {
    super(flow, MAX_CONCURRENCY_EXCEEDED, cause);
  }

}
