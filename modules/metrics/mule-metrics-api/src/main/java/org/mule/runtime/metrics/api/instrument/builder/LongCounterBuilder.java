/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.instrument.builder;

import org.mule.runtime.metrics.api.instrument.LongCounter;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builder class for {@link LongCounter}.
 *
 * @since 4.5.0
 */
public interface LongCounterBuilder extends InstrumentBuilder<LongCounter> {

  LongCounterBuilder NO_OP = new LongCounterBuilder() {

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
      return LongCounter.NO_OP;
    }

    @Override
    public LongCounterBuilder withAddOperation(BiConsumer<Long, Map<String, String>> addOperation) {
      return this;
    }

    @Override
    public LongCounterBuilder withIncrementAndGetOperation(Function<Map<String, String>, Long> incrementAndGetOperation) {
      return this;
    }

    @Override
    public LongCounterBuilder withValueSupplier(Supplier<Long> valueSupplier) {
      return this;
    }
  };

  /**
   * @param consumerForAddOperation the {@link Consumer} to be invoked when the {@link LongCounter#add(long)} is invoked.
   * @return {@link LongCounterBuilder}.
   */
  LongCounterBuilder withAddOperation(BiConsumer<Long, Map<String, String>> consumerForAddOperation);

  /**
   * @param incrementAndGetOperation the {@link Function} to be used when {@link LongCounter#incrementAndGetAsInt()} and
   *                                 {@link LongCounter#incrementAndGetAsLong()} are invoked.
   * @return the {@link LongCounterBuilder}.
   */
  LongCounterBuilder withIncrementAndGetOperation(Function<Map<String, String>, Long> incrementAndGetOperation);

  /**
   * @param valueSupplier the value {@link Supplier} to invoke when {@link LongCounter#getValueAsLong()} and
   *                      {@link LongCounter#getValueAsInt()} are invoked.
   * @return the corresponding {@link LongCounterBuilder}
   */
  LongCounterBuilder withValueSupplier(Supplier<Long> valueSupplier);
}
