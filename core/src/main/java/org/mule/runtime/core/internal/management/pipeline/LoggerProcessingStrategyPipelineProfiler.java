/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.management.pipeline;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.event.CoreEvent;
import org.slf4j.Logger;

import java.util.Optional;

import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An {@link ProcessingStrategyPipelineProfiler} that logs the actions involved.
 */
public class LoggerProcessingStrategyPipelineProfiler implements ProcessingStrategyPipelineProfiler {

  public static final String LOG_PROFILE_BEFORE_DISPATCHING_TO_FLOW_TEMPLATE =
      "Event with correlation id {} will be dispatch to {}";

  public static final String LOG_AFTER_PIPELINE_PROCESSED_TEMPLATE =
      "Event with correlation id {} finished process in {}";

  public static final String NOT_NAMED_PIPELINE_TAG = "NOT_NAMED_PIPELINE";

  private static final Logger LOGGER = getLogger(LoggerProcessingStrategyPipelineProfiler.class);

  private final Optional<ComponentLocation> location;

  private final String locationToLog;

  /**
   * @param location the name of the pipeline to profile.
   */
  public LoggerProcessingStrategyPipelineProfiler(ComponentLocation location) {
    this.location = ofNullable(location);
    this.locationToLog = this.location.map(loc -> loc.getLocation()).orElse(NOT_NAMED_PIPELINE_TAG);
  }

  @Override
  public void profileBeforeDispatchingToPipeline(CoreEvent e) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(LOG_PROFILE_BEFORE_DISPATCHING_TO_FLOW_TEMPLATE, e.getCorrelationId(), locationToLog);
    }
  }

  @Override
  public void profileAfterPipelineProcessed(CoreEvent e) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(LOG_AFTER_PIPELINE_PROCESSED_TEMPLATE, e.getCorrelationId(), locationToLog);
    }
  }
}
