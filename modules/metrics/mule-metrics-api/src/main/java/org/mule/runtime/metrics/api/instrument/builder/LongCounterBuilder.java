/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.api.instrument.builder;

import org.mule.runtime.metrics.api.instrument.LongCounter;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Builder class for {@link LongCounter}.
 *
 * @since 4.5.0
 */
public interface LongCounterBuilder extends InstrumentBuilder<LongCounter> {

  /**
   * @param consumerForAddOperation the {@link Consumer} to be invoked when the {@link LongCounter#add(long)} is invoked.
   * @return {@link LongCounterBuilder}.
   */
  LongCounterBuilder withConsumerForAddOperation(Consumer<Long> consumerForAddOperation);

  /**
   * @param supplierForIncrementAndGetOperation the {@link Supplier} to be used when {@link LongCounter#incrementAndGetAsInt()}
   *                                            and {@link LongCounter#incrementAndGetAsLong()} are invoked.
   * @return the {@link LongCounterBuilder}.
   */
  LongCounterBuilder withSupplierForIncrementAndGetOperation(Supplier<Long> supplierForIncrementAndGetOperation);

  /**
   * @param valueSupplier the value {@link Supplier} to invoke when {@link LongCounter#getValueAsLong()} and
   *                      {@link LongCounter#getValueAsInt()} are invoked.
   * @return the corresponding {@link LongCounterBuilder}
   */
  LongCounterBuilder withValueSupplier(Supplier<Long> valueSupplier);
}
