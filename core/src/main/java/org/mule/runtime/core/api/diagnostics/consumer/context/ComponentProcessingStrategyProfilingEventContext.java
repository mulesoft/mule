/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.diagnostics.consumer.context;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * A profiler context that encapsulates data for the profiling event.
 *
 * @since 4.4
 */
public class ComponentProcessingStrategyProfilingEventContext extends AbstractProcessingStrategyProfilingEventContext {

  private final Optional<ComponentLocation> location;

  public ComponentProcessingStrategyProfilingEventContext(CoreEvent e,
                                                          ComponentLocation location,
                                                          String threadName,
                                                          String artifactId,
                                                          String artifactType,
                                                          long profilingEventTimestamp) {
    super(e, threadName, artifactId, artifactType, profilingEventTimestamp);
    this.location = ofNullable(location);
  }

  /**
   * @return the optional {@link ComponentLocation} associated with the profiling event.
   */
  public Optional<ComponentLocation> getLocation() {
    return location;
  }
}
