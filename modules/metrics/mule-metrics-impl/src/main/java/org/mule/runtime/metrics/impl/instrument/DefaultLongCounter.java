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
import org.mule.runtime.metrics.impl.instrument.builder.LongCounterBuilderWithInstrumentRepository;
import org.mule.runtime.metrics.impl.instrument.repository.InstrumentRepository;

/**
 * An implementation of {@link LongCounter}.
 */
public class DefaultLongCounter implements LongCounter {

  public static LongCounterBuilderWithInstrumentRepository builder(String name) {
    return new DefaultLongCounterBuilder(name);
  }

  private final String name;
  private final String description;

  private final String unit;
  private long value;

  private DefaultLongCounter(String name, String description, String unit) {
    this.name = name;
    this.description = description;
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
    if (value < 0) {
      throw new IllegalArgumentException("The value to add must be positive");
    }
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

  private static class DefaultLongCounterBuilder implements LongCounterBuilderWithInstrumentRepository {

    private final String name;
    private InstrumentRepository instrumentRepository;
    private String description;
    private String unit;

    public DefaultLongCounterBuilder(String name) {
      this.name = name;
    }

    @Override
    public LongCounterBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    @Override
    public LongCounterBuilder withUnit(String unit) {
      this.unit = unit;
      return this;
    }

    @Override
    public LongCounter build() {
      return ofNullable(instrumentRepository)
          .map(repository -> (LongCounter) repository.register(name, name -> doBuild(name, description, unit)))
          .orElse(doBuild(name, description, unit));
    }

    private LongCounter doBuild(String name, String description, String unit) {
      return new DefaultLongCounter(name, description, unit);
    }

    @Override
    public LongCounterBuilderWithInstrumentRepository withInstrumentRepository(InstrumentRepository instrumentRepository) {
      this.instrumentRepository = instrumentRepository;
      return this;
    }
  }
}
