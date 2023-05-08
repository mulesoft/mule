/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
