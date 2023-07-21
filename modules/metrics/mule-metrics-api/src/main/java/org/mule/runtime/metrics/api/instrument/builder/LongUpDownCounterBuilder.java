/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.api.instrument.builder;

import org.mule.runtime.metrics.api.instrument.LongCounter;
import org.mule.runtime.metrics.api.instrument.LongUpDownCounter;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Builder class for {@link LongUpDownCounter}.
 *
 * @since 4.5.0
 */
public interface LongUpDownCounterBuilder extends InstrumentBuilder<LongUpDownCounter> {

  /**
   * Sets the initial value for the instrument to build.
   *
   * @param initialValue the initial value.
   *
   * @return the {@link LongCounterBuilder}.
   */
  LongUpDownCounterBuilder withInitialValue(long initialValue);

  /**
   * @param consumerForAddOperation the {@link Consumer} to be invoked when the {@link LongUpDownCounter#add(long)} is invoked.
   * @return {@link LongUpDownCounterBuilder}.
   */
  LongUpDownCounterBuilder withConsumerForAddOperation(Consumer<Long> consumerForAddOperation);

  /**
   * @param supplierForIncrementAndGetOperation the {@link Supplier} to be used when
   *                                            {@link LongUpDownCounter#incrementAndGetAsInt()} and
   *                                            {@link LongUpDownCounter#incrementAndGetAsLong()} are invoked.
   * @return the {@link LongUpDownCounterBuilder}.
   */
  LongUpDownCounterBuilder withSupplierForIncrementAndGetOperation(Supplier<Long> supplierForIncrementAndGetOperation);

  /**
   * @param supplierForDecrementAndGetOperation the {@link Supplier} to be used when
   *                                            {@link LongUpDownCounter#decrementAndGetAsInt()} ()} and
   *                                            {@link LongUpDownCounter#decrementAndGetAsLong()} are invoked.
   * @return the {@link LongUpDownCounterBuilder}.
   */
  LongUpDownCounterBuilder withSupplierForDecrementAndGetOperation(Supplier<Long> supplierForDecrementAndGetOperation);

  /**
   * @param valueSupplier the value {@link Supplier} to invoke when {@link LongCounter#getValueAsLong()} and
   *                      {@link LongCounter#getValueAsInt()} are invoked.
   * @return the corresponding {@link LongUpDownCounterBuilder}
   */
  LongUpDownCounterBuilder withValueSupplier(Supplier<Long> valueSupplier);
}
