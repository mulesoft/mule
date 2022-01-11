/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import org.mule.runtime.api.component.Component;

import java.util.concurrent.RejectedExecutionException;

import static org.mule.runtime.core.api.construct.BackPressureReason.REQUIRED_SCHEDULER_BUSY_WITH_FULL_BUFFER;

/**
 * The flow is already processing the number of events required by its maxConcurrency.
 *
 * @since 4.3, 4.2.2
 */
public class FlowBackPressureRequiredSchedulerBusyWithFullBufferException extends FlowBackPressureException {

  private static final long serialVersionUID = -7316630352445805505L;

  /**
   * Create a new {@link FlowBackPressureRequiredSchedulerBusyWithFullBufferException} with no cause. This is typically use when a
   * stream based processing exerts back-pressure without throwing an exception.
   */
  public FlowBackPressureRequiredSchedulerBusyWithFullBufferException(Component flow) {
    super(flow, REQUIRED_SCHEDULER_BUSY_WITH_FULL_BUFFER);
  }

  /**
   * Create a new {@link FlowBackPressureMaxConcurrencyExceededException} with a cause. This is typically use when a non-stream
   * based processing strategy is in use and back-pressure is identified by a {@link RejectedExecutionException}.
   */
  public FlowBackPressureRequiredSchedulerBusyWithFullBufferException(Component flow, Throwable cause) {
    super(flow, REQUIRED_SCHEDULER_BUSY_WITH_FULL_BUFFER, cause);
  }

}
