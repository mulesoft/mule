/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.context;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.type.context.ComponentExecutionProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.OperationThreadSnapshot;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.threading.DefaultOperationThreadSnapshot;

import java.util.Optional;

/**
 * {@link ComponentExecutionProfilingEventContext} default implementation.
 *
 * @since 4.4
 */
public class DefaultComponentExecutionProfilingEventContext implements ComponentExecutionProfilingEventContext {

  private final CoreEvent event;
  private final String artifactId;
  private final String artifactType;
  private final String threadName;
  private final long profilingEventTimestamp;
  private final Optional<ComponentLocation> location;
  private Optional<OperationThreadSnapshot> threadSnapshot;

  public DefaultComponentExecutionProfilingEventContext(CoreEvent event,
                                                        ComponentLocation location,
                                                        String threadName,
                                                        String artifactId,
                                                        String artifactType,
                                                        long profilingEventTimestamp) {
    this.event = event;
    this.threadName = threadName;
    this.artifactId = artifactId;
    this.artifactType = artifactType;
    this.profilingEventTimestamp = profilingEventTimestamp;
    this.location = ofNullable(location);
    // TODO: Replace by utility method.
    // TODO: Add a capability flag (could be something like "threading_profiling") has a separate task in order to check D&I of
    // such feature.
    this.threadSnapshot = empty();
  }

  @Override
  public String getCorrelationId() {
    return event.getCorrelationId();
  }

  @Override
  public String getThreadName() {
    return threadName;
  }

  @Override
  public Optional<OperationThreadSnapshot> getThreadSnapshot() {
    return threadSnapshot;
  }

  @Override
  public String getArtifactId() {
    return artifactId;
  }

  @Override
  public String getArtifactType() {
    return artifactType;
  }

  @Override
  public long getTriggerTimestamp() {
    return profilingEventTimestamp;
  }

  @Override
  public Optional<ComponentLocation> getLocation() {
    return location;
  }

  public void setThreadSnapshot(DefaultOperationThreadSnapshot threadSnapshot) {
    this.threadSnapshot = ofNullable(threadSnapshot);
  }
}
