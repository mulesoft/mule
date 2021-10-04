/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.context;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * A {@link org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext} that encapsulates data
 * for the profiling event.
 *
 * @since 4.4
 */
public class DefaultComponentProcessingStrategyProfilingEventContext implements ComponentProcessingStrategyProfilingEventContext {

  private final CoreEvent event;
  private final String artifactId;
  private final String artifactType;
  private final String threadName;
  private final long profilingEventTimestamp;
  private final Optional<ComponentLocation> location;

  public DefaultComponentProcessingStrategyProfilingEventContext(CoreEvent event,
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
  }

  @Override
  public String getCorrelationId() {
    return event.getCorrelationId();
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

  @Override
  public long getTriggerTimestamp() {
    return profilingEventTimestamp;
  }

  public Optional<ComponentLocation> getLocation() {
    return location;
  }

}
