/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.management.execution;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Long.MAX_VALUE;
import static java.lang.System.nanoTime;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An execution profiler that logs the actions related to the dispatching of a {@link CoreEvent} through a processing strategy. It
 * logs the action, the event correlation id and the time taken since the beginning of the operation.
 * 
 * @since 4.4.0, 4.3.1
 */
public class LoggerProcessingStrategyExecutionProfiler implements ProcessingStrategyExecutionProfiler {

  public static final String LOG_BEFORE_DISPATCHING_TO_PROCESSOR_TEMPLATE =
      "Event with correlation id {} and processing type {} is about to be dispatched for processing to {}. Start time {}";

  public static final String LOG_BEFORE_COMPONENT_PROCESSING_TEMPLATE =
      "Event with correlation id {} and processing type {} was dispatched for processing in {}. About to be processed. Dispatched time {}";

  public static final String LOG_AFTER_DISPATCHING_TO_FLOW =
      "Event with correlation id {} was dispatched again to the main flow from {}. Dispatched time to flow {}. Full processing time {} ";

  public static final String LOG_AFTER_RESPONSE_RECEIVED =
      "Response for event with correlation id {} was received. About to dispatch to the main flow. Response time {}";

  public static final String UNKNOWN_LOCATION_TAG = "UNKNOWN";

  private static final Logger LOGGER = getLogger(LoggerProcessingStrategyExecutionProfiler.class);

  private final String locationToLog;

  private final ProcessingType processingType;

  private AtomicLong startTime = new AtomicLong(MAX_VALUE);

  private AtomicLong dispatchTime = new AtomicLong(MAX_VALUE);

  private AtomicLong responseTime = new AtomicLong(MAX_VALUE);

  private Optional<ComponentLocation> location;

  public LoggerProcessingStrategyExecutionProfiler(ComponentLocation location, ProcessingType processingType) {
    this.location = getLocation(location);
    this.processingType = processingType;
    this.locationToLog = this.location.map(ComponentLocation::getLocation).orElse(UNKNOWN_LOCATION_TAG);
  }

  private Optional<ComponentLocation> getLocation(ComponentLocation location) {
    return ofNullable(location);
  }

  @Override
  public void profileBeforeDispatchingToProcessor(CoreEvent e) {
    startTime.set(nanoTime());
    log(LOG_BEFORE_DISPATCHING_TO_PROCESSOR_TEMPLATE, e.getCorrelationId(), processingType.toString(), locationToLog,
        Long.toString(nanoTime()));
  }

  @Override
  public void profileBeforeComponentProcessing(CoreEvent e) {
    dispatchTime.set(nanoTime());
    log(LOG_BEFORE_COMPONENT_PROCESSING_TEMPLATE, e.getCorrelationId(), processingType.toString(), locationToLog,
        Long.toString(dispatchTime.get() - startTime.get()));
  }

  @Override
  public void profileAfterResponseReceived(CoreEvent e) {
    responseTime.set(nanoTime());
    log(LOG_AFTER_RESPONSE_RECEIVED, e.getCorrelationId(), processingType.toString(), locationToLog,
        Long.toString(responseTime.get() - dispatchTime.get()));
  }

  @Override
  public void profileAfterDispatchingToFlow(CoreEvent e) {
    log(LOG_AFTER_DISPATCHING_TO_FLOW,
        e.getCorrelationId(), locationToLog, Long.toString(responseTime.get() - nanoTime()),
        Long.toString(nanoTime() - startTime.get()));
  }

  protected void log(String template, String... parameters) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(template, parameters);
    }
  }
}
