/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl.meter;

import static java.util.Optional.ofNullable;

import org.mule.runtime.metrics.api.instrument.builder.LongCounterBuilder;
import org.mule.runtime.metrics.api.instrument.builder.LongUpDownCounterBuilder;
import org.mule.runtime.metrics.api.meter.Meter;
import org.mule.runtime.metrics.api.meter.builder.MeterBuilder;
import org.mule.runtime.metrics.impl.instrument.DefaultLongCounter;
import org.mule.runtime.metrics.impl.instrument.DefaultLongUpDownCounter;
import org.mule.runtime.metrics.impl.instrument.repository.InstrumentRepository;
import org.mule.runtime.metrics.impl.meter.builder.MeterBuilderWithRepository;
import org.mule.runtime.metrics.impl.meter.repository.MeterRepository;

/**
 * A default implementation of a {@link Meter}
 */
public class DefaultMeter implements Meter {

  public static MeterBuilderWithRepository builder(String meterName) {
    return new DefaultMeterBuilder(meterName);
  }

  private final String name;
  private final String description;

  private final InstrumentRepository instrumentRepository = new InstrumentRepository();

  public DefaultMeter(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public LongUpDownCounterBuilder upDownCounterBuilder(String name) {
    return DefaultLongUpDownCounter.builder(name).withInstrumentRepository(instrumentRepository);
  }

  @Override
  public LongCounterBuilder counterBuilder(String name) {
    return DefaultLongCounter.builder(name).withInstrumentRepository(instrumentRepository);
  }

  private static class DefaultMeterBuilder implements MeterBuilderWithRepository {

    private final String meterName;
    private String description;
    private MeterRepository meterRepository;

    public DefaultMeterBuilder(String meterName) {
      this.meterName = meterName;
    }

    @Override
    public MeterBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    @Override
    public Meter build() {
      return ofNullable(meterRepository).map(repository -> repository.create(meterName, name -> doBuild(name, description)))
          .orElse(doBuild(meterName, description));
    }

    private Meter doBuild(String meterName, String description) {
      return new DefaultMeter(meterName, description);
    }

    @Override
    public MeterBuilder withMeterRepository(MeterRepository meterRepository) {
      this.meterRepository = meterRepository;
      return this;
    }
  }
}
