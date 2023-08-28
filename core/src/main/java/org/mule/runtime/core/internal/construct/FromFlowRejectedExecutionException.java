/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

  private static final long serialVersionUID = 4393720264347573011L;

  private final BackPressureReason reason;

  public FromFlowRejectedExecutionException(BackPressureReason reason) {
    this.reason = reason;
  }


  public BackPressureReason getReason() {
    return reason;
  }
}
