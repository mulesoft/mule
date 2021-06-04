/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.management.execution;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.event.CoreEvent;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.nanoTime;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An execution profiler that logs the actions related to the dispatching of a {@link CoreEvent} through a processing strategy. It
 * logs the action, the event correlation id and the time taken since the beginning of the operation.
 * 
 * @since 4.4.0, 4.3.1
 */
public class LoggerProcessingStrategyExecutionProfiler implements ProcessingStrategyExecutionProfiler {

  public static final String LOG_BEFORE_DISPATCHING_TO_PROCESSOR_TEMPLATE =
      "Event with correlation id {} is about to be dispatched for processing to {}";

  public static final String LOG_BEFORE_COMPONENT_PROCESSING_TEMPLATE =
      "Event with correlation id {} was dispatched for processing in {}. About to be processed.";

  public static final String LOG_AFTER_DISPATCHING_TO_FLOW =
      "Event with correlation id {} was dispatched again to the main flow from {}. Processing time {} ";

  public static final String UNKNOWN_LOCATION_TAG = "UNKNOWN";

  private static final String LOG_AFTER_RESPONSE_RECEIVED =
      "Response for event with correlation id {} was received. About to dispatch to the main flow.";

  private static final Logger LOGGER = getLogger(LoggerProcessingStrategyExecutionProfiler.class);

  private final String locationToLog;

  private AtomicLong startTime = new AtomicLong(Long.MAX_VALUE);
  private Optional<ComponentLocation> location;

  public LoggerProcessingStrategyExecutionProfiler(ComponentLocation location) {
    this.location = getLocation(location);
    this.locationToLog = this.location.map(ComponentLocation::getLocation).orElse(UNKNOWN_LOCATION_TAG);
  }

  private Optional<ComponentLocation> getLocation(ComponentLocation location) {
    return Optional.ofNullable(location);
  }

  @Override
  public void profileBeforeDispatchingToProcessor(CoreEvent e) {
    log(LOG_BEFORE_DISPATCHING_TO_PROCESSOR_TEMPLATE, e.getCorrelationId(), locationToLog);
  }

  @Override
  public void profileBeforeComponentProcessing(CoreEvent e) {
    log(LOG_BEFORE_COMPONENT_PROCESSING_TEMPLATE, e.getCorrelationId(), locationToLog);
  }

  @Override
  public void profileAfterResponseReceived(CoreEvent e) {
    log(LOG_AFTER_RESPONSE_RECEIVED, e.getCorrelationId(), locationToLog);
  }

  @Override
  public void profileAfterDispatchingToFlow(CoreEvent e) {
    log(LOG_AFTER_DISPATCHING_TO_FLOW,
        e.getCorrelationId(), locationToLog, Long.toString(nanoTime() - startTime.get()));
  }

  protected void log(String template, String... parameters) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(template, parameters);
    }
  }
}
