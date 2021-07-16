/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.diagnostics.notification;

import org.mule.api.annotation.Experimental;
import org.mule.runtime.core.api.diagnostics.ProfilingEventType;
import org.mule.runtime.core.api.diagnostics.consumer.context.ProcessingStrategyProfilingEventContext;

import java.util.Objects;

/**
 * A {@link ProfilingEventType} for processing strategies.
 *
 * @since 4.4
 */
@Experimental
public class ProcessingStrategyProfilingEventType implements ProfilingEventType<ProcessingStrategyProfilingEventContext> {

  private final String name;

  public static ProcessingStrategyProfilingEventType of(String name) {
    return new ProcessingStrategyProfilingEventType(name);
  }

  private ProcessingStrategyProfilingEventType(String name) {
    this.name = name;
  }

  @Override
  public String getProfilingEventName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ProcessingStrategyProfilingEventType that = (ProcessingStrategyProfilingEventType) o;
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
