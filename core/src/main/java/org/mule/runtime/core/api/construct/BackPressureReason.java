/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.construct;

import org.mule.runtime.core.internal.construct.FlowBackPressureException;

/**
 * Provides a set of known reasons for which a {@link FlowBackPressureException} may be thrown.
 *
 * @since 4.3, 4.2.2
 */
public enum BackPressureReason {

  /**
   * The flow is already processing the number of events required by its maxConcurrency.
   */
  MAX_CONCURRENCY_EXCEEDED,

  /**
   * At least one of the schedulers required by the flow is unable to accept new tasks.
   */
  REQUIRED_SCHEDULER_BUSY,

  /**
   * At least one of the schedulers required by the flow is unable to accept new tasks and the flow's buffer to accept exceeding
   * tasks is full.
   */
  REQUIRED_SCHEDULER_BUSY_WITH_FULL_BUFFER,

  /**
   * The flow did not consume enough of the previous events.
   */
  EVENTS_ACCUMULATED;

}
