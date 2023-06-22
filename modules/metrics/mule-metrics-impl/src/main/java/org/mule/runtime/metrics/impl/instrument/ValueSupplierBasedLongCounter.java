/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl.instrument;

import static java.util.Objects.requireNonNull;

import org.mule.runtime.metrics.api.instrument.LongCounter;
import org.mule.runtime.metrics.api.instrument.registration.LongCounterRegistrationHelper;
import org.mule.runtime.metrics.api.meter.Meter;
import org.mule.runtime.metrics.exporter.api.MeterExporter;
import org.mule.runtime.metrics.impl.instrument.repository.InstrumentRepository;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A {@link LongCounter} that is based in a {@link Supplier<Long>} to retrieve the value. It is used so that the counter itself is
 * managed outside.
 */
public class ValueSupplierBasedLongCounter implements LongCounter {

  public static ValueSupplierBasedWithInstrumentRepositoryHelper getValueSupplierBasedLongCounterRegistrationWithInstrumentRepositoryHelper(String name,
                                                                                                                                            Meter meter) {
    return new ValueSupplierBasedWithInstrumentRepositoryHelper(name, meter);
  }

  private final String name;
  private final String description;
  private final String unit;
  private final Supplier<Long> valueSupplier;

  private final Consumer<Long> additionAndGetOperation;
  private final Supplier<Long> incrementAndGetOperation;

  private final Meter meter;

  private ValueSupplierBasedLongCounter(String name,
                                        String description,
                                        String unit,
                                        Meter meter,
                                        Supplier<Long> valueSupplier,
                                        Consumer<Long> additionAndGetOperation,
                                        Supplier<Long> incrementAndGetOperation) {
    this.name = name;
    this.description = description;
    this.unit = unit;
    this.meter = meter;
    this.valueSupplier = valueSupplier;
    this.additionAndGetOperation = additionAndGetOperation;
    this.incrementAndGetOperation = incrementAndGetOperation;
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
    if (value < 0) {
      throw new IllegalArgumentException("The value to add must be positive");
    }
    additionAndGetOperation.accept(value);
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
  public Meter getMeter() {
    return meter;
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


  public static class ValueSupplierBasedWithInstrumentRepositoryHelper implements LongCounterRegistrationHelper {

    private final String name;
    private final Meter meter;

    private InstrumentRepository instrumentRepository;
    private MeterExporter meterExporter;
    private String description;
    private String unit;
    private Consumer<Long> addOperation;
    private Supplier<Long> incrementAndGetOperation;
    private Supplier<Long> valueSupplier;

    public ValueSupplierBasedWithInstrumentRepositoryHelper(String name, Meter meter) {
      this.name = name;
      this.meter = meter;
    }

    @Override
    public ValueSupplierBasedWithInstrumentRepositoryHelper withDescription(String description) {
      this.description = description;
      return this;
    }

    @Override
    public ValueSupplierBasedWithInstrumentRepositoryHelper withUnit(String unit) {
      this.unit = unit;
      return this;
    }

    @Override
    public LongCounter register() {
      requireNonNull(valueSupplier);
      requireNonNull(addOperation);
      requireNonNull(incrementAndGetOperation);
      LongCounter longCounter = null;
      if (instrumentRepository != null) {
        longCounter = (LongCounter) instrumentRepository.create(name,
                                                                (name) -> new ValueSupplierBasedLongCounter(name,
                                                                                                            description,
                                                                                                            unit,
                                                                                                            meter,
                                                                                                            valueSupplier,
                                                                                                            addOperation,
                                                                                                            incrementAndGetOperation));
      } else {
        longCounter = new ValueSupplierBasedLongCounter(name,
                                                        description,
                                                        unit,
                                                        meter,
                                                        valueSupplier,
                                                        addOperation,
                                                        incrementAndGetOperation);
      }

      if (meterExporter != null) {
        meterExporter.enableExport(longCounter);
      }

      return longCounter;
    }

    @Override
    public ValueSupplierBasedWithInstrumentRepositoryHelper withConsumerForAddOperation(Consumer<Long> consumerForAddOperation) {
      this.addOperation = consumerForAddOperation;
      return this;
    }

    @Override
    public ValueSupplierBasedWithInstrumentRepositoryHelper withSupplierForIncrementAndGetOperation(Supplier<Long> supplierForIncrementOperation) {
      this.incrementAndGetOperation = supplierForIncrementOperation;
      return this;
    }

    @Override
    public ValueSupplierBasedWithInstrumentRepositoryHelper withValueSupplier(Supplier<Long> valueSupplier) {
      this.valueSupplier = valueSupplier;
      return this;
    }

    public ValueSupplierBasedWithInstrumentRepositoryHelper withInstrumentRepository(InstrumentRepository instrumentRepository) {
      this.instrumentRepository = instrumentRepository;
      return this;
    }

    public ValueSupplierBasedWithInstrumentRepositoryHelper withMeterExporter(MeterExporter meterExporter) {
      this.meterExporter = meterExporter;
      return this;
    }
  }
}
