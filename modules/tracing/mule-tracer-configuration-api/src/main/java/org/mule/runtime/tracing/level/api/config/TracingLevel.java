/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracing.level.api.config;

public class TracingLevel {

  private final boolean isOverride;
  private final TracingLevelId tracingLevelId;

  public TracingLevel(boolean isOverride, TracingLevelId tracingLevelId) {
    this.isOverride = isOverride;
    this.tracingLevelId = tracingLevelId;
  }

  public boolean isOverride() {
    return isOverride;
  }

  public TracingLevelId getTracingLevelId() {
    return tracingLevelId;
  }
}
