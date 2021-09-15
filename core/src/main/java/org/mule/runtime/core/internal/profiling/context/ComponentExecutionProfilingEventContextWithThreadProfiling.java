/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.context;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.type.context.ComponentExecutionProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.OperationThreadSnapshot;

import java.util.Optional;

public class ComponentExecutionProfilingEventContextWithThreadProfiling implements ComponentExecutionProfilingEventContext {

  private final ComponentExecutionProfilingEventContext delegate;
  private final Optional<OperationThreadSnapshot> threadSnapshot;

  public ComponentExecutionProfilingEventContextWithThreadProfiling(ComponentExecutionProfilingEventContext delegate,
                                                                    OperationThreadSnapshot threadSnapshot) {
    this.delegate = delegate;
    this.threadSnapshot = ofNullable(threadSnapshot);
  }

  @Override
  public long getTriggerTimestamp() {
    return delegate.getTriggerTimestamp();
  }

  @Override
  public String getCorrelationId() {
    return delegate.getCorrelationId();
  }

  @Override
  public String getThreadName() {
    return delegate.getThreadName();
  }

  @Override
  public Optional<OperationThreadSnapshot> getThreadSnapshot() {
    return threadSnapshot;
  }

  @Override
  public String getArtifactId() {
    return delegate.getArtifactId();
  }

  @Override
  public String getArtifactType() {
    return delegate.getArtifactType();
  }

  @Override
  public Optional<ComponentLocation> getLocation() {
    return delegate.getLocation();
  }
}
