/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl;

import static org.mule.runtime.metrics.impl.meter.DefaultMeter.builder;

import org.mule.runtime.metrics.api.MeterProvider;
import org.mule.runtime.metrics.api.meter.builder.MeterBuilder;
import org.mule.runtime.metrics.impl.meter.repository.MeterRepository;

/**
 * A default implementation of the {@link MeterProvider}
 */
public class DefaultMeterProvider implements MeterProvider {

  MeterRepository meterRepository = new MeterRepository();

  @Override
  public MeterBuilder getMeterBuilder(String meterName) {
    return builder(meterName).withMeterRepository(meterRepository);
  }

  public MeterRepository getMeterRepository() {
    return meterRepository;
  }
}
