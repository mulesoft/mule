/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl.instrument.repository;

import org.mule.runtime.metrics.api.instrument.Instrument;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A repository for {@link Instrument}.
 *
 * @since 4.5.0
 */
public class InstrumentRepository {

  Map<String, Instrument> instrumentMap = new HashMap<>();

  /**
   * @param name            the name of the {@link Instrument}
   * @param builderFunction the builder function to create {@link Instrument} if not present.
   */
  public Instrument create(String name, Function<String, Instrument> builderFunction) {
    return instrumentMap.computeIfAbsent(name, builderFunction);
  }
}
