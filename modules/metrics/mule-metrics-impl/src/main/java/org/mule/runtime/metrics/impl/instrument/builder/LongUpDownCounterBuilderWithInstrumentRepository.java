/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl.instrument.builder;

import org.mule.runtime.metrics.api.instrument.builder.LongUpDownCounterBuilder;
import org.mule.runtime.metrics.impl.instrument.repository.InstrumentRepository;

/**
 * A builder that can accept an {@link InstrumentRepository}
 */
public interface LongUpDownCounterBuilderWithInstrumentRepository extends LongUpDownCounterBuilder {

  /**
   * @param instrumentRepository the instrument repository.
   * @return the corresponding {@link LongUpDownCounterBuilderWithInstrumentRepository}.
   */
  LongUpDownCounterBuilderWithInstrumentRepository withInstrumentRepository(InstrumentRepository instrumentRepository);
}
