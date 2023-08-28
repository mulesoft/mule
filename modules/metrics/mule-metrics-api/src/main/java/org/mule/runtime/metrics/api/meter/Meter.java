/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
