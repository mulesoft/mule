/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method;

import static org.mule.runtime.tracer.impl.DefaultCoreEventTracerUtils.safeExecute;
import static org.mule.runtime.tracer.impl.DefaultCoreEventTracerUtils.safeExecuteWithDefaultOnThrowable;

import static java.util.Optional.empty;

import org.mule.runtime.tracer.api.span.InternalSpan;

import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;

/**
 * An operation to perform in a fail safe mode.
 *
 * @since 4.5.0
 */
public class FailsafeDistributedTraceContextOperation {

  private final boolean propagateExceptions;
  private final Logger customLogger;
  private final String errorMessage;

  private FailsafeDistributedTraceContextOperation(Logger customLogger,
                                                   String errorMessage,
                                                   boolean propagateExceptions) {
    this.customLogger = customLogger;
    this.errorMessage = errorMessage;
    this.propagateExceptions = propagateExceptions;
  }

  public static FailsafeDistributedTraceContextOperation getFailsafeDistributedTraceContextOperation(Logger customLogger,
                                                                                                     String errorMessage,
                                                                                                     boolean propagateExceptions) {
    return new FailsafeDistributedTraceContextOperation(customLogger, errorMessage, propagateExceptions);
  }

  public Optional<InternalSpan> execute(Supplier<Optional<InternalSpan>> internalSpanSupplier) {
    return safeExecuteWithDefaultOnThrowable(internalSpanSupplier::get,
                                             empty(),
                                             errorMessage,
                                             propagateExceptions,
                                             customLogger);
  }

  public void execute(Runnable runnable) {
    safeExecute(runnable::run, errorMessage, propagateExceptions, customLogger);
  }

}
