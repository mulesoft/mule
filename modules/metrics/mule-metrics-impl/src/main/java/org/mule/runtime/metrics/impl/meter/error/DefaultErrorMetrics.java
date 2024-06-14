/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl.meter.error;

import static java.util.Objects.requireNonNull;
import static org.mule.runtime.api.exception.ExceptionHelper.getRootException;
import static java.util.Collections.singletonMap;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.metrics.api.MeterProvider;
import org.mule.runtime.metrics.api.internal.error.ErrorMetrics;
import org.mule.runtime.metrics.api.instrument.LongCounter;
import org.mule.runtime.metrics.api.meter.Meter;
import org.mule.runtime.metrics.impl.util.StackHasher;

public class DefaultErrorMetrics implements ErrorMetrics {

  private final Meter errorMeter;
  private final LongCounter totalErrorsCounter = buildNewErrorCounter();

  // TODO: Make an ErrorIdGenerator interface in order to encapsulate the stack hash implementation
  private final StackHasher errorIdGenerator = new StackHasher();

  public DefaultErrorMetrics(MeterProvider<Meter> errorMeterProvider) {
    requireNonNull(errorMeterProvider);
    this.errorMeter =
        errorMeterProvider.getMeterBuilder("mule-error-meter").withDescription("Mule runtime execution errors").build();
  }

  @Override
  public void measure(Error error) {
    totalErrorsCounter.add(1, singletonMap("error-id", getErrorId(error)));
  }

  @Override
  public void measure(Throwable error) {
    totalErrorsCounter.add(1, singletonMap("error-id", getErrorId(error)));
  }

  private LongCounter buildNewErrorCounter() {
    return errorMeter.counterBuilder("error-count")
        .withDescription("Mule runtime error count").build();
  }

  private String getErrorId(Error error) {
    return errorIdGenerator.hexHash(getRootException(error.getCause()));
  }

  private String getErrorId(Throwable error) {
    return errorIdGenerator.hexHash(getRootException(error));
  }

}
