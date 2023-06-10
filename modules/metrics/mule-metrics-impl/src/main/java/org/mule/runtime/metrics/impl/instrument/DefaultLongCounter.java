/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl.instrument;

import static java.util.Optional.ofNullable;

import org.mule.runtime.metrics.api.instrument.LongCounter;
import org.mule.runtime.metrics.api.instrument.builder.LongCounterBuilder;
import org.mule.runtime.metrics.api.meter.Meter;
import org.mule.runtime.metrics.impl.instrument.repository.InstrumentRepository;
import org.mule.runtime.metrics.exporter.api.MeterExporter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * An implementation of {@link LongCounter}.
 */
public class DefaultLongCounter implements LongCounter {

  public static DefaultLongCounterBuilder builder(String name, Meter meter) {
    return new DefaultLongCounterBuilder(name, meter);
  }

  private final AtomicLong value = new AtomicLong(0);
  private final String name;
  private final String description;
  private final String unit;
  private Meter meter;

  private DefaultLongCounter(String name, String description, String unit, Meter meter) {
    this.name = name;
    this.description = description;
    this.unit = unit;
    this.meter = meter;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public Meter getMeter() {
    return meter;
  }

  @Override
  public void add(long value) {
    if (value < 0) {
      throw new IllegalArgumentException("The value to add must be positive");
    }
    this.value.addAndGet(value);
  }

  @Override
  public long getValueAsLong() {
    return value.longValue();
  }

  @Override
  public String getUnit() {
    return unit;
  }

  @Override
  public int getValueAsInt() {
    return value.intValue();
  }

  @Override
  public int incrementAndGetAsInt() {
    return (int) value.incrementAndGet();
  }

  @Override
  public long incrementAndGetAsLong() {
    return value.incrementAndGet();
  }

  @Override
  public void reset() {
    value.set(0L);
  }

  public static class DefaultLongCounterBuilder implements LongCounterBuilder {

    private final String name;
    private final Meter meter;
    private InstrumentRepository instrumentRepository;
    private String description;
    private String unit = "";
    private MeterExporter meterExporter;

    public DefaultLongCounterBuilder(String name, Meter meter) {
      this.name = name;
      this.meter = meter;
    }

    @Override
    public DefaultLongCounterBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    @Override
    public DefaultLongCounterBuilder withUnit(String unit) {
      this.unit = unit;
      return this;
    }

    public DefaultLongCounterBuilder withInstrumentRepository(InstrumentRepository instrumentRepository) {
      this.instrumentRepository = instrumentRepository;
      return this;
    }

    public DefaultLongCounterBuilder withMeterExporter(MeterExporter meterExporter) {
      this.meterExporter = meterExporter;
      return this;
    }

    @Override
    public LongCounter build() {
      LongCounter longCounter = ofNullable(instrumentRepository)
          .map(repository -> (LongCounter) repository.create(name, name -> doBuild(name, description, unit, meter)))
          .orElse(doBuild(name, description, unit, meter));

      if (meterExporter != null) {
        meterExporter.enableExport(longCounter);
      }

      return longCounter;
    }

    private LongCounter doBuild(String name, String description, String unit, Meter meter) {
      return new DefaultLongCounter(name, description, unit, meter);
    }
  }
}
