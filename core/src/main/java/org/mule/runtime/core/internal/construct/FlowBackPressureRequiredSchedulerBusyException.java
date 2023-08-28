/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.construct;

import org.mule.runtime.api.component.Component;

import java.util.concurrent.RejectedExecutionException;

import static org.mule.runtime.core.api.construct.BackPressureReason.REQUIRED_SCHEDULER_BUSY;

/**
 * The flow is already processing the number of events required by its maxConcurrency.
 *
 * @since 4.3, 4.2.2
 */
public class FlowBackPressureRequiredSchedulerBusyException extends FlowBackPressureException {

  private static final long serialVersionUID = -2762699363241225531L;

  /**
   * Create a new {@link FlowBackPressureRequiredSchedulerBusyException} with no cause. This is typically use when a stream based
   * processing exerts back-pressure without throwing an exception.
   */
  public FlowBackPressureRequiredSchedulerBusyException(Component flow) {
    super(flow, REQUIRED_SCHEDULER_BUSY);
  }

  /**
   * Create a new {@link FlowBackPressureRequiredSchedulerBusyException} with a cause. This is typically use when a non-stream
   * based processing strategy is in use and back-pressure is identified by a {@link RejectedExecutionException}.
   */
  public FlowBackPressureRequiredSchedulerBusyException(Component flow, Throwable cause) {
    super(flow, REQUIRED_SCHEDULER_BUSY, cause);
  }

}
