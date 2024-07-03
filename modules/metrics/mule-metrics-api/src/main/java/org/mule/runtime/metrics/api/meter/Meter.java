/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.meter;

import org.mule.runtime.metrics.api.instrument.builder.LongCounterBuilder;
import org.mule.runtime.metrics.api.instrument.builder.LongUpDownCounterBuilder;

import java.util.function.BiConsumer;

/**
 * Provides instruments used to record measurements which are aggregated to metrics.
 *
 * <p>
 * Instruments are obtained through builders provided by this interface.
 *
 * @since 4.5.0
 **/
public interface Meter {

  /**
   * No operation {@link Meter} implementation.
   */
  Meter NO_OP = new Meter() {

    @Override
    public String getName() {
      return "NO_OP";
    }

    @Override
    public String getDescription() {
      return "NO_OP";
    }

    @Override
    public void forEachAttribute(BiConsumer<String, String> biConsumer) {
      // Nothing to do.
    }

    @Override
    public LongUpDownCounterBuilder upDownCounterBuilder(String name) {
      return LongUpDownCounterBuilder.NO_OP;
    }

    @Override
    public LongCounterBuilder counterBuilder(String name) {
      return LongCounterBuilder.NO_OP;
    }
  };

  /**
   * @return name of the meter.
   */
  String getName();

  /**
   * @return description of the meter.
   */
  String getDescription();

  /**
   * Applies the consumer for each meter attribute.
   *
   * @param biConsumer the {@link BiConsumer} to apply for each attribute.
   */
  void forEachAttribute(BiConsumer<String, String> biConsumer);

  /**
   * @param name the name of the instrument.
   *
   * @return the {@link LongUpDownCounterBuilder}
   */
  LongUpDownCounterBuilder upDownCounterBuilder(String name);

  /**
   * @param name the name of the instrument.
   * @return the {@link LongCounterBuilder}
   */
  LongCounterBuilder counterBuilder(String name);

}
