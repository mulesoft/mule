/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import org.mule.runtime.core.api.construct.BackPressureReason;

import java.util.concurrent.RejectedExecutionException;

/**
 * The flow is already processing the number of events required by its maxConcurrency.
 *
 * @since 4.3, 4.2.2
 */
public class FlowBackPressureEventsAccumulatedException extends FlowBackPressureException {

  private static final long serialVersionUID = 7149961265248945243L;

  /**
   * Create a new {@link FlowBackPressureEventsAccumulatedException} with no cause. This is typically use when a
   * stream based processing exerts back-pressure without throwing an exception.
   */
  public FlowBackPressureEventsAccumulatedException(String flowName, BackPressureReason reason) {
    super(flowName, reason);
  }

  /**
   * Create a new {@link FlowBackPressureEventsAccumulatedException} with a cause. This is typically use when a non-stream
   * based processing strategy is in use and back-pressure is identified by a {@link RejectedExecutionException}.
   */
  public FlowBackPressureEventsAccumulatedException(String flowName, BackPressureReason reason, Throwable cause) {
    super(flowName, reason, cause);
  }

}
