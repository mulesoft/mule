/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.BackPressureReason;

import java.util.concurrent.RejectedExecutionException;

/**
 * Exception thrown when a {@link org.mule.runtime.core.api.construct.Flow} exerts back-pressure.
 *
 * @since 4.1
 */
public abstract class FlowBackPressureException extends MuleException {

  static String BACK_PRESSURE_ERROR_MESSAGE = "Flow '%s' is unable to accept new events at this time. Reason: %s";
  private static final long serialVersionUID = -4973370165925845336L;

  /**
   * Create a new {@link FlowBackPressureException} with no cause. This is typically use when a stream based processing exerts
   * back-pressure without throwing an exception.
   */
  public FlowBackPressureException(String flowName, BackPressureReason reason) {
    super(createStaticMessage(BACK_PRESSURE_ERROR_MESSAGE, flowName, reason.toString()));
  }

  /**
   * Create a new {@link FlowBackPressureException} with a cause. This is typically use when a non-stream based processing
   * strategy is in use and back-pressure is identified by a {@link RejectedExecutionException}.
   */
  public FlowBackPressureException(String flowName, BackPressureReason reason, Throwable cause) {
    super(createStaticMessage(BACK_PRESSURE_ERROR_MESSAGE, flowName, reason.toString()), cause);
  }

  public static FlowBackPressureException createFlowBackPressureException(String flowName, BackPressureReason reason) {
    switch (reason) {
      case MAX_CONCURRENCY_EXCEEDED:
        return new FlowBackPressureMaxConcurrencyExceededException(flowName, reason);
      case REQUIRED_SCHEDULER_BUSY:
        return new FlowBackPressureRequiredSchedulerBusyException(flowName, reason);
      case REQUIRED_SCHEDULER_BUSY_BUFFER_FULL:
        return new FlowBackPressureRequiredSchedulerBusyBufferFullException(flowName, reason);
      case EVENTS_ACCUMULATED:
        return new FlowBackPressureEventsAccumulatedException(flowName, reason);
      default:
        return null;
    }
  }

  public static void createAndThrow(String flowName, BackPressureReason reason) throws FlowBackPressureException {
    final FlowBackPressureException toThrow = createFlowBackPressureException(flowName, reason);
    if (toThrow != null) {
      throw toThrow;
    }
  }

  public static FlowBackPressureException createFlowBackPressureException(String flowName, BackPressureReason reason,
                                                                          Throwable cause) {
    switch (reason) {
      case MAX_CONCURRENCY_EXCEEDED:
        return new FlowBackPressureMaxConcurrencyExceededException(flowName, reason, cause);
      case REQUIRED_SCHEDULER_BUSY:
        return new FlowBackPressureRequiredSchedulerBusyException(flowName, reason, cause);
      case REQUIRED_SCHEDULER_BUSY_BUFFER_FULL:
        return new FlowBackPressureRequiredSchedulerBusyBufferFullException(flowName, reason, cause);
      case EVENTS_ACCUMULATED:
        return new FlowBackPressureEventsAccumulatedException(flowName, reason, cause);
      default:
        throw new IllegalArgumentException("Cannot build a FlowBackPressureException with a cause without a reason");
    }
  }

  public static void createAndThrow(String flowName, BackPressureReason reason, Throwable cause)
      throws FlowBackPressureException {
    final FlowBackPressureException toThrow = createFlowBackPressureException(flowName, reason, cause);
    if (toThrow != null) {
      throw toThrow;
    }
  }
}
