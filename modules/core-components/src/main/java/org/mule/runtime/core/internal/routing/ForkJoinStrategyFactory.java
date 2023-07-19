/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.routing;

import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.privileged.routing.CompositeRoutingException;


public interface ForkJoinStrategyFactory {

  /**
   * Create instance of {@link ForkJoinStrategy}
   *
   * @param processingStrategy   processing strategy to use
   * @param maxConcurrency       maximum number of routes/parts to be processed in parallel.
   * @param delayErrors          if all routers/parts should be processed regardless of errors and a
   *                             {@link CompositeRoutingException} thrown or not.
   * @param timeoutErrorType     the timeout error type.
   * @param isDetailedLogEnabled if detailed error/exception message will be provided or not.
   * @return new instance of {@link ForkJoinStrategy}
   */
  ForkJoinStrategy createForkJoinStrategy(ProcessingStrategy processingStrategy, int maxConcurrency, boolean delayErrors,
                                          long timeout, Scheduler timeoutScheduler,
                                          ErrorType timeoutErrorType, boolean isDetailedLogEnabled);

  DataType getResultDataType();

}
