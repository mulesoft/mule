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
 * Specialization of {@link RejectedExecutionException} that also contains a {@link BackPressureReason}.
 *
 * @since 4.3, 4.2.2
 */
public class FromFlowRejectedExecutionException extends RejectedExecutionException {

  private static final long serialVersionUID = 4393720264347573009L;

  private final BackPressureReason reason;

  public FromFlowRejectedExecutionException(BackPressureReason reason) {
    this.reason = reason;
  }


  public BackPressureReason getReason() {
    return reason;
  }
}
