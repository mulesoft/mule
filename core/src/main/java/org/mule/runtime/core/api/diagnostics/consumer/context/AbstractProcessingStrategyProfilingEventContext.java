/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.diagnostics.consumer.context;

import org.mule.runtime.core.api.diagnostics.consumer.context.ProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * Abstract class for general context for processing strategy profiling data.
 *
 * @since 4.4
 */
public abstract class AbstractProcessingStrategyProfilingEventContext implements ProcessingStrategyProfilingEventContext {

  private final CoreEvent e;
  private final String artifactId;
  private final String artifactType;
  private String threadName;
  private long profilingEventTimestamp;

  public AbstractProcessingStrategyProfilingEventContext(CoreEvent e,
                                                         String threadName,
                                                         String artifactId,
                                                         String artifactType,
                                                         long profilingEventTimestamp) {
    this.e = e;
    this.threadName = threadName;
    this.artifactId = artifactId;
    this.artifactType = artifactType;
    this.profilingEventTimestamp = profilingEventTimestamp;
  }

  /**
   * @return the {@link CoreEvent} associated with the profiling event.
   */
  public CoreEvent getEvent() {
    return e;
  }

  /**
   * @return the thread name of the profiling event.
   */
  public String getThreadName() {
    return threadName;
  }

  /**
   * @return the artifact id of the profiling event.
   */
  public String getArtifactId() {
    return artifactId;
  }

  /**
   * @return the artifact type of the profiling event.
   */
  public String getArtifactType() {
    return artifactType;
  }

  public long getTimestamp() {
    return profilingEventTimestamp;
  }

}
