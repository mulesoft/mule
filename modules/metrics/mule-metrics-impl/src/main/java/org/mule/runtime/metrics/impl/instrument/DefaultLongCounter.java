/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
import java.util.function.Consumer;
import java.util.function.Supplier;

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
  private final Meter meter;

  private Supplier<Long> valueSupplier = value::get;

  private Consumer<Long> addOperation = value::addAndGet;

  private Supplier<Long> incrementAndGetOperation = value::incrementAndGet;

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

    addOperation.accept(value);
  }

  @Override
  public long getValueAsLong() {
    return valueSupplier.get();
  }

  @Override
  public String getUnit() {
    return unit;
  }

  @Override
  public int getValueAsInt() {
    return valueSupplier.get().intValue();
  }

  @Override
  public int incrementAndGetAsInt() {
    return incrementAndGetOperation.get().intValue();
  }

  @Override
  public long incrementAndGetAsLong() {
    return incrementAndGetOperation.get();
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
    private String unit;
    private MeterExporter meterExporter;

    private Consumer<Long> addOperation;
    private Supplier<Long> incrementAndGetOperation;
    private Supplier<Long> valueSupplier;

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
          .map(repository -> (LongCounter) repository.create(name, name -> doBuild()))
          .orElse(doBuild());

      if (meterExporter != null) {
        meterExporter.enableExport(longCounter);
      }

      return longCounter;
    }

    private LongCounter doBuild() {
      DefaultLongCounter longCounter = new DefaultLongCounter(name, description, unit, meter);

      if (valueSupplier != null) {
        longCounter.setValueSupplier(valueSupplier);
      }

      if (addOperation != null) {
        longCounter.setAddOperation(addOperation);
      }

      if (incrementAndGetOperation != null) {
        longCounter.setIncrementAndGetOperation(incrementAndGetOperation);
      }

      return longCounter;
    }

    @Override
    public DefaultLongCounterBuilder withConsumerForAddOperation(Consumer<Long> consumerForAddOperation) {
      this.addOperation = consumerForAddOperation;
      return this;
    }

    @Override
    public DefaultLongCounterBuilder withSupplierForIncrementAndGetOperation(Supplier<Long> supplierForIncrementOperation) {
      this.incrementAndGetOperation = supplierForIncrementOperation;
      return this;
    }

    @Override
    public DefaultLongCounterBuilder withValueSupplier(Supplier<Long> valueSupplier) {
      this.valueSupplier = valueSupplier;
      return this;
    }
  }

  private void setIncrementAndGetOperation(Supplier<Long> incrementAndGetOperation) {
    this.incrementAndGetOperation = incrementAndGetOperation;
  }

  private void setAddOperation(Consumer<Long> addOperation) {
    this.addOperation = addOperation;
  }

  private void setValueSupplier(Supplier<Long> valueSupplier) {
    this.valueSupplier = valueSupplier;
  }
}
