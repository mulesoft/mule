/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing;

import org.mule.runtime.api.profiling.tracing.TaskTracingContext;
import org.mule.runtime.api.profiling.tracing.TaskTracingService;

public class ThreadLocalTaskTracingService implements TaskTracingService {

  private static final ThreadLocal<TaskTracingContext> currentTaskTracingContext = new ThreadLocal<>();

  public ThreadLocalTaskTracingService() {}

  @Override
  public TaskTracingContext getCurrentTaskTracingContext() {
    return currentTaskTracingContext.get();
  }

  @Override
  public void deleteCurrentTaskTracingContext() {
    currentTaskTracingContext.remove();
  }

  @Override
  public TaskTracingContext setCurrentTaskTracingContext(TaskTracingContext taskTracingContext) {
    // Since TaskTracingContext is immutable, it can be set as is (it would require creating a copy otherwise).
    currentTaskTracingContext.set(taskTracingContext);
    return taskTracingContext;
  }
}
