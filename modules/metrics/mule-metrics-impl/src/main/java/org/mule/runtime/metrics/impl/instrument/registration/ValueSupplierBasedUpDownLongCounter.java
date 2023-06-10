/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl.instrument.registration;

import org.mule.runtime.metrics.api.instrument.LongUpDownCounter;
import org.mule.runtime.metrics.api.instrument.registration.InstrumentRegistrationHelper;
import org.mule.runtime.metrics.api.instrument.registration.LongUpDownCounterRegistrationHelper;
import org.mule.runtime.metrics.api.meter.Meter;
import org.mule.runtime.metrics.impl.instrument.repository.InstrumentRepository;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A {@link LongUpDownCounter} that is based in a {@link Supplier<Long>} to retrieve the value. It is used so that the counter
 * itself is managed outside.
 */
public class ValueSupplierBasedUpDownLongCounter implements LongUpDownCounter {

  public static LongUpDownCounterRegistrationHelper getValueSupplierBasedLongUpDownCounterRegistrationWithInstrumentRepositoryHelper(String name,
                                                                                                                                     Meter meter,
                                                                                                                                     InstrumentRepository instrumentRepository) {
    return new ValueSupplierLongUpDownCounterRegistrationWithInstrumentRepositoryHelper(name, meter, instrumentRepository);
  }

  private final String name;
  private final Meter meter;
  private final String description;
  private final String unit;
  private final Supplier<Long> valueSupplier;
  private final Consumer<Long> addOperation;
  private final Supplier<Long> incrementAndGetOperation;
  private final Supplier<Long> decrementAndGetOpertion;

  private ValueSupplierBasedUpDownLongCounter(String name,
                                              Meter meter,
                                              String description,
                                              String unit,
                                              Supplier<Long> valueSupplier,
                                              Consumer<Long> addOperation,
                                              Supplier<Long> incrementAndGetOperation,
                                              Supplier<Long> decrementAndGetOperation) {
    this.name = name;
    this.meter = meter;
    this.description = description;
    this.unit = unit;
    this.valueSupplier = valueSupplier;
    this.addOperation = addOperation;
    this.incrementAndGetOperation = incrementAndGetOperation;
    this.decrementAndGetOpertion = decrementAndGetOperation;
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
    addOperation.accept(value);
  }

  @Override
  public long getValueAsLong() {
    return valueSupplier.get();
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
    return incrementAndGetOperation.get().intValue();
  }

  @Override
  public long incrementAndGetAsLong() {
    return incrementAndGetOperation.get();
  }

  @Override
  public int decrementAndGetAsInt() {
    return decrementAndGetOpertion.get().intValue();
  }

  @Override
  public long decrementAndGetAsLong() {
    return decrementAndGetOpertion.get();
  }

  private static class ValueSupplierLongUpDownCounterRegistrationWithInstrumentRepositoryHelper
      implements LongUpDownCounterRegistrationHelper {

    private final String name;
    private final Meter meter;
    private final InstrumentRepository instrumentRepository;
    private String description;
    private String unit = "";
    private Consumer<Long> addOperation;
    private Supplier<Long> incrementOperation;
    private Supplier<Long> decrementAndGetOperation;
    private Supplier<Long> valueSupplier;

    public ValueSupplierLongUpDownCounterRegistrationWithInstrumentRepositoryHelper(String name, Meter meter,
                                                                                    InstrumentRepository instrumentRepository) {
      this.name = name;
      this.meter = meter;
      this.instrumentRepository = instrumentRepository;
    }

    @Override
    public InstrumentRegistrationHelper<LongUpDownCounter> withDescription(String description) {
      this.description = description;
      return this;
    }

    @Override
    public InstrumentRegistrationHelper<LongUpDownCounter> withUnit(String unit) {
      this.unit = unit;
      return this;
    }

    @Override
    public LongUpDownCounter register() {
      return (LongUpDownCounter) instrumentRepository
          .create(name,
                  (name) -> new ValueSupplierBasedUpDownLongCounter(name, meter, description, unit, valueSupplier,
                                                                    addOperation, incrementOperation, decrementAndGetOperation));
    }

    @Override
    public LongUpDownCounterRegistrationHelper withConsumerForAddOperation(Consumer<Long> additionOperation) {
      this.addOperation = additionOperation;
      return this;
    }

    @Override
    public LongUpDownCounterRegistrationHelper withSupplierForIncrementAndGetOperation(Supplier<Long> incrementOperation) {
      this.incrementOperation = incrementOperation;
      return this;
    }

    @Override
    public LongUpDownCounterRegistrationHelper withSupplierForDecrementAndGetOperation(Supplier<Long> decrementOperation) {
      this.decrementAndGetOperation = decrementOperation;
      return this;
    }

  }
}
