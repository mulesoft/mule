/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import static java.lang.String.format;

import java.util.concurrent.RejectedExecutionException;

/**
 * Exception thrown when a {@link org.mule.runtime.core.api.construct.Flow} exerts back-pressure.
 *
 * @since 4.1
 */
public class FlowBackPressureException extends Exception {

  static String BACK_PRESSURE_ERROR_MESSAGE = "Flow '%s' is unable to accept new events at this time";
  private static final long serialVersionUID = 2992038225993918910L;

  /**
   * Create a new {@link FlowBackPressureException} with no cause. This is typically use when a stream based processing exerts
   * back-pressure without throwing an exception.
   * 
   */
  public FlowBackPressureException(String flowName) {
    super(format(BACK_PRESSURE_ERROR_MESSAGE, flowName));
  }

  /**
   * Create a new {@link FlowBackPressureException} with a cause. This is typically use when a non-stream based processing
   * strategy is in use and back-pressure is identified by a a {@link RejectedExecutionException}.
   *
   */
  public FlowBackPressureException(String flowName, Throwable cause) {
    super(format(BACK_PRESSURE_ERROR_MESSAGE, flowName), cause);
  }

}
