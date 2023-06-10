/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.instrument.registration;

import org.mule.runtime.metrics.api.instrument.LongUpDownCounter;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A helper to create a {@link LongUpDownCounter}
 *
 * @since 4.5.0
 */
public interface LongUpDownCounterRegistrationHelper extends InstrumentRegistrationHelper<LongUpDownCounter> {

  /**
   * @param consumerForAddOperation the {@link Consumer} to be invoked when the {@link LongUpDownCounter#add(long)} is invoked.
   * @return {@link LongUpDownCounterRegistrationHelper}.
   */
  LongUpDownCounterRegistrationHelper withConsumerForAddOperation(Consumer<Long> consumerForAddOperation);

  /**
   * @param supplierForIncrementAndGetOperation the {@link Supplier} to be used when
   *                                            {@link LongUpDownCounter#incrementAndGetAsInt()} and
   *                                            {@link LongUpDownCounter#incrementAndGetAsLong()} are invoked.
   * @return the {@link LongUpDownCounterRegistrationHelper}.
   */
  LongUpDownCounterRegistrationHelper withSupplierForIncrementAndGetOperation(Supplier<Long> supplierForIncrementAndGetOperation);

  /**
   * @param supplierForDecrementAndGetOperation the {@link Supplier} to be used when
   *                                            {@link LongUpDownCounter#decrementAndGetAsInt()} ()} and
   *                                            {@link LongUpDownCounter#decrementAndGetAsLong()} are invoked.
   * @return the {@link LongUpDownCounterRegistrationHelper}.
   */
  LongUpDownCounterRegistrationHelper withSupplierForDecrementAndGetOperation(Supplier<Long> supplierForDecrementAndGetOperation);


}
