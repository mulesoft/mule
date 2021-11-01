/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing;

import org.mule.runtime.api.profiling.tracing.TracingContext;
import org.mule.runtime.api.profiling.tracing.TracingService;

public class ThreadLocalTaskTracingService implements TracingService {

  private static final ThreadLocal<TracingContext> currentTracingContext = new ThreadLocal<>();

  public ThreadLocalTaskTracingService() {}

  @Override
  public TracingContext getCurrentTracingContext() {
    return currentTracingContext.get();
  }

  @Override
  public void deleteCurrentTracingContext() {
    currentTracingContext.remove();
  }

  @Override
  public TracingContext setCurrentTracingContext(TracingContext tracingContext) {
    // Since TracingContext is immutable, it can be set as is (it would require creating a copy otherwise).
    currentTracingContext.set(tracingContext);
    return tracingContext;
  }
}
