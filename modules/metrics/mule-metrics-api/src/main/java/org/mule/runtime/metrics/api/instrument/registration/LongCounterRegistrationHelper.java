/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.instrument.registration;

import org.mule.runtime.metrics.api.instrument.LongCounter;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A helper to create a {@link LongCounter}
 */
public interface LongCounterRegistrationHelper extends InstrumentRegistrationHelper<LongCounter> {

  /**
   * @param consumerForAddOperation the {@link Consumer} to be invoked when the {@link LongCounter#add(long)} is invoked.
   * @return {@link LongCounterRegistrationHelper}.
   */
  LongCounterRegistrationHelper withConsumerForAddOperation(Consumer<Long> consumerForAddOperation);

  /**
   * @param supplierForIncrementAndGetOperation the {@link Supplier} to be used when {@link LongCounter#incrementAndGetAsInt()}
   *                                            and {@link LongCounter#incrementAndGetAsLong()} are invoked.
   * @return the {@link LongCounterRegistrationHelper}.
   */
  LongCounterRegistrationHelper withSupplierForIncrementAndGetOperation(Supplier<Long> supplierForIncrementAndGetOperation);

  /**
   * @param valueSupplier the value {@link Supplier} to invoke when {@link LongCounter#getValueAsLong()} and
   *                      {@link LongCounter#getValueAsInt()} are invoked.
   * @return the corresponding {@link LongCounterRegistrationHelper}
   */
  LongCounterRegistrationHelper withValueSupplier(Supplier<Long> valueSupplier);

}
