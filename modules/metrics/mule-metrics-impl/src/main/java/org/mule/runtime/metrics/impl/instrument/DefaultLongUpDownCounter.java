/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl.instrument;

import static java.util.Optional.ofNullable;

import org.mule.runtime.metrics.api.instrument.LongUpDownCounter;
import org.mule.runtime.metrics.api.instrument.builder.LongUpDownCounterBuilder;
import org.mule.runtime.metrics.impl.instrument.builder.LongUpDownCounterBuilderWithInstrumentRepository;
import org.mule.runtime.metrics.impl.instrument.repository.InstrumentRepository;

/**
 * An implementation of {@link LongUpDownCounter}.
 */
public class DefaultLongUpDownCounter implements LongUpDownCounter {

  public static LongUpDownCounterBuilderWithInstrumentRepository builder(String name) {
    return new DefaultLongUpDownCounterBuilder(name);
  }

  private final String name;
  private final String description;
  private final String unit;
  private long value;

  private DefaultLongUpDownCounter(String name, String description, String unit, long initialValue) {
    this.name = name;
    this.description = description;
    this.value = initialValue;
    this.unit = unit;
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
    this.value += value;
  }

  @Override
  public long getValue() {
    return value;
  }

  @Override
  public String getUnit() {
    return unit;
  }

  private static class DefaultLongUpDownCounterBuilder implements LongUpDownCounterBuilderWithInstrumentRepository {

    private final String name;
    private InstrumentRepository instrumentRepository;
    private String description;
    private String unit;
    private long initialValue;

    public DefaultLongUpDownCounterBuilder(String name) {
      this.name = name;
    }

    @Override
    public LongUpDownCounterBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    @Override
    public LongUpDownCounterBuilder withUnit(String unit) {
      this.unit = unit;
      return this;
    }

    @Override
    public LongUpDownCounterBuilder withInitialValue(long initialValue) {
      this.initialValue = initialValue;
      return this;
    }

    @Override
    public LongUpDownCounter build() {
      return ofNullable(instrumentRepository)
          .map(repository -> (LongUpDownCounter) repository.create(name,
                                                                   name -> doBuild(name, description, unit, initialValue)))
          .orElse(doBuild(name, description, unit, initialValue));
    }

    private LongUpDownCounter doBuild(String name, String description, String unit, long initialValue) {
      return new DefaultLongUpDownCounter(name, description, unit, initialValue);
    }

    @Override
    public LongUpDownCounterBuilderWithInstrumentRepository withInstrumentRepository(InstrumentRepository instrumentRepository) {
      this.instrumentRepository = instrumentRepository;
      return this;
    }
  }
}
