/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.diagnostics.consumer.context;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.diagnostics.consumer.context.ProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * A profiler context that encapsulates data for the profiling event.
 *
 * @since 4.4
 */
public class ComponentProcessingStrategyProfilingEventContext implements ProcessingStrategyProfilingEventContext {

  private final CoreEvent e;
  private final String artifactId;
  private final String artifactType;
  private String threadName;
  private long profilingEventTimestamp;
  private final Optional<ComponentLocation> location;

  public ComponentProcessingStrategyProfilingEventContext(CoreEvent e,
                                                          ComponentLocation location,
                                                          String threadName,
                                                          String artifactId,
                                                          String artifactType,
                                                          long profilingEventTimestamp) {
    this.e = e;
    this.threadName = threadName;
    this.artifactId = artifactId;
    this.artifactType = artifactType;
    this.profilingEventTimestamp = profilingEventTimestamp;
    this.location = ofNullable(location);
  }

  public CoreEvent getEvent() {
    return e;
  }

  public String getThreadName() {
    return threadName;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getArtifactType() {
    return artifactType;
  }

  public long getTimestamp() {
    return profilingEventTimestamp;
  }

  public Optional<ComponentLocation> getLocation() {
    return location;
  }

}
