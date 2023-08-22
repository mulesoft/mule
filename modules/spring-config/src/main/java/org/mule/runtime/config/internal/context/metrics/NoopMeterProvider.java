/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context.metrics;

import org.mule.runtime.metrics.api.MeterProvider;
import org.mule.runtime.metrics.api.instrument.LongCounter;
import org.mule.runtime.metrics.api.instrument.LongUpDownCounter;
import org.mule.runtime.metrics.api.instrument.builder.LongCounterBuilder;
import org.mule.runtime.metrics.api.instrument.builder.LongUpDownCounterBuilder;
import org.mule.runtime.metrics.api.meter.Meter;
import org.mule.runtime.metrics.api.meter.builder.MeterBuilder;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Noop classes in case metering is not enabled.
 */
public class NoopMeterProvider implements MeterProvider {

  public static final String NOOP = "NOOP";
  private final MeterBuilder METER_BUILDER_INSTANCE = new NoopMeterBuilder();

  @Override
  public MeterBuilder getMeterBuilder(String meterName) {
    return METER_BUILDER_INSTANCE;
  }

  private static class NoopMeterBuilder implements MeterBuilder {

    private final LongUpDownCounterBuilder LONG_UP_DOWN_COUNTER_BUILDER_INSTANCE = new NoopLongUpDownCounterBuilder();
    private final LongCounterBuilder LONG_COUNTER_BUILDER_INSTANCE = new NoopLongCounterBuilder();

    @Override
    public Meter build() {
      return new NoopMeter();
    }

    @Override
    public MeterBuilder withDescription(String description) {
      return this;
    }

    @Override
    public MeterBuilder withMeterAttribute(String key, String value) {
      return this;
    }

    private class NoopMeter implements Meter {

      @Override
      public String getName() {
        return NOOP;
      }

      @Override
      public String getDescription() {
        return NOOP;
      }

      @Override
      public void forEachAttribute(BiConsumer<String, String> biConsumer) {
        // Nothing to do.
      }

      @Override
      public LongUpDownCounterBuilder upDownCounterBuilder(String name) {
        return LONG_UP_DOWN_COUNTER_BUILDER_INSTANCE;
      }

      @Override
      public LongCounterBuilder counterBuilder(String name) {
        return LONG_COUNTER_BUILDER_INSTANCE;
      }
    }

    private class NoopLongUpDownCounterBuilder implements LongUpDownCounterBuilder {

      private final LongUpDownCounter NOOP_LONG_UP_DOWN_COUNTER = new NoopLongUpDownCounter();

      @Override
      public LongUpDownCounterBuilder withDescription(String description) {
        return this;
      }

      @Override
      public LongUpDownCounterBuilder withUnit(String unit) {
        return this;
      }

      @Override
      public LongUpDownCounterBuilder withInitialValue(long initialValue) {
        return this;
      }

      @Override
      public LongUpDownCounterBuilder withConsumerForAddOperation(Consumer<Long> consumerForAddOperation) {
        return this;
      }

      @Override
      public LongUpDownCounterBuilder withSupplierForIncrementAndGetOperation(Supplier<Long> supplierForIncrementAndGetOperation) {
        return this;
      }

      @Override
      public LongUpDownCounterBuilder withSupplierForDecrementAndGetOperation(Supplier<Long> supplierForDecrementAndGetOperation) {
        return this;
      }

      @Override
      public LongUpDownCounterBuilder withValueSupplier(Supplier<Long> valueSupplier) {
        return this;
      }

      @Override
      public LongUpDownCounter build() {
        return NOOP_LONG_UP_DOWN_COUNTER;
      }

      private class NoopLongUpDownCounter implements LongUpDownCounter {

        public static final String NOOP = "NOOP";

        @Override
        public String getName() {
          return NOOP;
        }

        @Override
        public String getDescription() {
          return NOOP;
        }

        @Override
        public Meter getMeter() {
          return new NoopMeter();
        }

        @Override
        public void add(long value) {

        }

        @Override
        public long getValueAsLong() {
          return 0;
        }

        @Override
        public int getValueAsInt() {
          return 0;
        }

        @Override
        public String getUnit() {
          return NOOP;
        }

        @Override
        public int incrementAndGetAsInt() {
          return 0;
        }

        @Override
        public long incrementAndGetAsLong() {
          return 0;
        }

        @Override
        public int decrementAndGetAsInt() {
          return 0;
        }

        @Override
        public long decrementAndGetAsLong() {
          return 0;
        }
      }
    }

    private class NoopLongCounterBuilder implements LongCounterBuilder {

      private final LongCounter NOOP_LONG_COUNTER = new NoopLongCounter();

      @Override
      public LongCounterBuilder withDescription(String description) {
        return this;
      }

      @Override
      public LongCounterBuilder withUnit(String unit) {
        return this;
      }

      @Override
      public LongCounter build() {
        return NOOP_LONG_COUNTER;
      }

      @Override
      public LongCounterBuilder withConsumerForAddOperation(Consumer<Long> consumerForAddOperation) {
        return this;
      }

      @Override
      public LongCounterBuilder withSupplierForIncrementAndGetOperation(Supplier<Long> supplierForIncrementAndGetOperation) {
        return this;
      }

      @Override
      public LongCounterBuilder withValueSupplier(Supplier<Long> valueSupplier) {
        return this;
      }

      private class NoopLongCounter implements LongCounter {

        @Override
        public String getName() {
          return NOOP;
        }

        @Override
        public String getDescription() {
          return NOOP;
        }

        @Override
        public Meter getMeter() {
          return new NoopMeter();
        }

        @Override
        public void add(long value) {

        }

        @Override
        public long getValueAsLong() {
          return 0;
        }

        @Override
        public String getUnit() {
          return NOOP;
        }

        @Override
        public int getValueAsInt() {
          return 0;
        }

        @Override
        public int incrementAndGetAsInt() {
          return 0;
        }

        @Override
        public long incrementAndGetAsLong() {
          return 0;
        }
      }
    }
  }
}
