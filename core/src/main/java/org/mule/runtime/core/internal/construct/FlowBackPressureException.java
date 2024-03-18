/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.backpressure;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.BackPressureReason;

import java.util.concurrent.RejectedExecutionException;

/**
 * Exception thrown when a {@link org.mule.runtime.core.api.construct.Flow} exerts back-pressure.
 *
 * @since 4.1
 */
public abstract class FlowBackPressureException extends MuleException {

  private static final long serialVersionUID = -4973370165925845336L;

  private final Component flow;

  /**
   * Create a new {@link FlowBackPressureException} with no cause. This is typically use when a stream based processing exerts
   * back-pressure without throwing an exception.
   */
  public FlowBackPressureException(Component flow, BackPressureReason reason) {
    super(backpressure(flow.getLocation().getRootContainerName(), reason));
    this.flow = flow;
  }

  /**
   * Create a new {@link FlowBackPressureException} with a cause. This is typically use when a non-stream based processing
   * strategy is in use and back-pressure is identified by a {@link RejectedExecutionException}.
   */
  public FlowBackPressureException(Component flow, BackPressureReason reason, Throwable cause) {
    super(backpressure(flow.getLocation().getRootContainerName(), reason), cause);
    this.flow = flow;
  }

  /**
   * @return An instance of the right type of {@link FlowBackPressureException} according to the given {@link BackPressureReason}.
   */
  public static FlowBackPressureException createFlowBackPressureException(Component flow, BackPressureReason reason) {
    return createFlowBackPressureException(flow, reason, null);
  }

  /**
   * @return An instance of the right type of {@link FlowBackPressureException} according to the given {@link BackPressureReason}.
   */
  public static FlowBackPressureException createFlowBackPressureException(Component flow, BackPressureReason reason,
                                                                          Throwable cause) {
    switch (reason) {
      case MAX_CONCURRENCY_EXCEEDED:
        return new FlowBackPressureMaxConcurrencyExceededException(flow, cause);
      case REQUIRED_SCHEDULER_BUSY:
        return new FlowBackPressureRequiredSchedulerBusyException(flow, cause);
      case REQUIRED_SCHEDULER_BUSY_WITH_FULL_BUFFER:
        return new FlowBackPressureRequiredSchedulerBusyWithFullBufferException(flow, cause);
      case EVENTS_ACCUMULATED:
        return new FlowBackPressureEventsAccumulatedException(flow, cause);
      default:
        throw new IllegalArgumentException("Cannot build a FlowBackPressureException with a cause without a reason");
    }
  }

  public static void createAndThrowIfNeeded(Component flow, BackPressureReason reason, Throwable cause)
      throws FlowBackPressureException {
    final FlowBackPressureException toThrow = createFlowBackPressureException(flow, reason, cause);
    if (toThrow != null) {
      throw toThrow;
    }
  }

  /**
   * @return the flow for which backpressure was applied.
   */
  public Component getFlow() {
    return flow;
  }
}
