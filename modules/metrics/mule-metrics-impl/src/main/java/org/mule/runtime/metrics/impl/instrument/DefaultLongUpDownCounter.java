/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl.instrument;

import static java.util.Optional.ofNullable;

import org.mule.runtime.metrics.api.instrument.LongUpDownCounter;
import org.mule.runtime.metrics.api.instrument.builder.LongUpDownCounterBuilder;
import org.mule.runtime.metrics.api.meter.Meter;
import org.mule.runtime.metrics.impl.instrument.repository.InstrumentRepository;
import org.mule.runtime.metrics.exporter.api.MeterExporter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An implementation of {@link LongUpDownCounter}.
 */
public class DefaultLongUpDownCounter implements LongUpDownCounter {

  public static DefaultLongUpDownCounterBuilder builder(String name, Meter meter) {
    return new DefaultLongUpDownCounterBuilder(name, meter);
  }

  private final String name;
  private final String description;

  private final long initialValue;
  private final String unit;
  private final Meter meter;
  private final AtomicLong value;

  private Consumer<Long> consumerForAddOperation;
  private Supplier<Long> supplierForIncrementAndGetOperation;
  private Supplier<Long> supplierForDecrementAndGetOperation;
  private Supplier<Long> valueSupplier;

  private DefaultLongUpDownCounter(String name, String description, String unit, long initialValue, Meter meter) {
    this.name = name;
    this.description = description;
    this.initialValue = initialValue;
    this.value = new AtomicLong(initialValue);
    this.valueSupplier = value::get;
    this.supplierForDecrementAndGetOperation = value::decrementAndGet;
    this.supplierForIncrementAndGetOperation = value::incrementAndGet;
    this.consumerForAddOperation = value::addAndGet;
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
  public void add(long value) {
    this.consumerForAddOperation.accept(value);
  }

  @Override
  public long getValueAsLong() {
    return valueSupplier.get().longValue();
  }

  @Override
  public int getValueAsInt() {
    return valueSupplier.get().intValue();
  }

  @Override
  public String getUnit() {
    return unit;
  }

  @Override
  public Meter getMeter() {
    return meter;
  }

  @Override
  public int incrementAndGetAsInt() {
    return supplierForIncrementAndGetOperation.get().intValue();
  }

  @Override
  public long incrementAndGetAsLong() {
    return supplierForIncrementAndGetOperation.get();
  }

  @Override
  public int decrementAndGetAsInt() {
    return supplierForDecrementAndGetOperation.get().intValue();
  }

  @Override
  public long decrementAndGetAsLong() {
    return supplierForDecrementAndGetOperation.get();
  }

  private void setValueSupplier(Supplier<Long> valueSupplier) {
    this.valueSupplier = valueSupplier;
  }

  private void setSupplierForDecrementAndGetOperation(Supplier<Long> supplierForDecrementAndGetOperation) {
    this.supplierForDecrementAndGetOperation = supplierForDecrementAndGetOperation;
  }

  private void setSupplierForIncrementAndGetOperation(Supplier<Long> supplierForIncrementAndGetOperation) {
    this.supplierForIncrementAndGetOperation = supplierForIncrementAndGetOperation;
  }

  private void setConsumerForAddOperation(Consumer<Long> consumerForAddOperation) {
    this.consumerForAddOperation = consumerForAddOperation;
  }

  @Override
  public void reset() {
    value.set(initialValue);
  }

  public static class DefaultLongUpDownCounterBuilder implements LongUpDownCounterBuilder {

    private final String name;

    private final Meter meter;
    private InstrumentRepository instrumentRepository;
    private String description;
    private String unit;
    private long initialValue;
    private MeterExporter meterExporter;

    private Consumer<Long> consumerForAddOperation;
    private Supplier<Long> supplierForIncrementAndGetOperation;
    private Supplier<Long> supplierForDecrementAndGetOperation;
    private Supplier<Long> valueSupplier;

    public DefaultLongUpDownCounterBuilder(String name, Meter meter) {
      this.name = name;
      this.meter = meter;
    }

    @Override
    public DefaultLongUpDownCounterBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    @Override
    public DefaultLongUpDownCounterBuilder withUnit(String unit) {
      this.unit = unit;
      return this;
    }

    @Override
    public DefaultLongUpDownCounterBuilder withInitialValue(long initialValue) {
      this.initialValue = initialValue;
      return this;
    }

    public DefaultLongUpDownCounterBuilder withInstrumentRepository(InstrumentRepository instrumentRepository) {
      this.instrumentRepository = instrumentRepository;
      return this;
    }

    public DefaultLongUpDownCounterBuilder withMeterExporter(MeterExporter meterExporter) {
      this.meterExporter = meterExporter;
      return this;
    }

    @Override
    public DefaultLongUpDownCounterBuilder withConsumerForAddOperation(Consumer<Long> consumerForAddOperation) {
      this.consumerForAddOperation = consumerForAddOperation;
      return this;
    }

    @Override
    public DefaultLongUpDownCounterBuilder withSupplierForIncrementAndGetOperation(Supplier<Long> supplierForIncrementAndGetOperation) {
      this.supplierForIncrementAndGetOperation = supplierForIncrementAndGetOperation;
      return this;
    }

    @Override
    public DefaultLongUpDownCounterBuilder withSupplierForDecrementAndGetOperation(Supplier<Long> supplierForDecrementAndGetOperation) {
      this.supplierForDecrementAndGetOperation = supplierForDecrementAndGetOperation;
      return this;
    }

    @Override
    public LongUpDownCounterBuilder withValueSupplier(Supplier<Long> valueSupplier) {
      this.valueSupplier = valueSupplier;
      return this;
    }

    @Override
    public LongUpDownCounter build() {
      LongUpDownCounter longUpDownCounter = ofNullable(instrumentRepository)
          .map(repository -> (LongUpDownCounter) repository.create(name,
                                                                   name -> doBuild(name, description, unit, initialValue,
                                                                                   meter)))
          .orElse(doBuild(name, description, unit, initialValue, meter));

      if (meterExporter != null) {
        meterExporter.enableExport(longUpDownCounter);
      }

      return longUpDownCounter;
    }

    private LongUpDownCounter doBuild(String name, String description, String unit, long initialValue, Meter meter) {
      DefaultLongUpDownCounter longUpDownCounter = new DefaultLongUpDownCounter(name, description, unit, initialValue, meter);

      if (consumerForAddOperation != null) {
        longUpDownCounter.setConsumerForAddOperation(consumerForAddOperation);
      }

      if (supplierForIncrementAndGetOperation != null) {
        longUpDownCounter.setSupplierForIncrementAndGetOperation(supplierForIncrementAndGetOperation);
      }

      if (supplierForDecrementAndGetOperation != null) {
        longUpDownCounter.setSupplierForDecrementAndGetOperation(supplierForDecrementAndGetOperation);
      }

      if (valueSupplier != null) {
        longUpDownCounter.setValueSupplier(valueSupplier);
      }

      return longUpDownCounter;
    }

  }
}
