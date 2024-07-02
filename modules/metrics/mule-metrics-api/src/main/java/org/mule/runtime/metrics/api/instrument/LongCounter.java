/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.instrument;

import org.mule.runtime.metrics.api.meter.Meter;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A counter for registering long measurements, only incremental.
 *
 * @since 4.5.0
 */
public interface LongCounter extends Instrument {

  LongCounter NO_OP = new LongCounter() {

    @Override
    public String getName() {
      return "NO_OP";
    }

    @Override
    public String getDescription() {
      return "NO_OP";
    }

    @Override
    public Meter getMeter() {
      return Meter.NO_OP;
    }

    @Override
    public void add(long value) {
      // Nothing to do
    }

    @Override
    public void add(long value, Map<String, String> attributes) {
      // Nothing to do
    }

    @Override
    public int incrementAndGetAsInt() {
      return 0;
    }

    @Override
    public long incrementAndGetAsLong() {
      return 0;
    }

    @Override
    public long getValueAsLong() {
      return 0;
    }

    @Override
    public String getUnit() {
      return "";
    }

    @Override
    public int getValueAsInt() {
      return 0;
    }

    @Override
    public void onAddition(BiConsumer<Long, Map<String, String>> consumer) {
      // Nothing to do
    }
  };

  /**
   * Add a value. Should only be positive.
   *
   * @param value
   */
  void add(long value);

  void add(long value, Map<String, String> attributes);

  /**
   * Increments the counter and gets the value as an int.
   *
   * @return the resulting value as int.
   */
  int incrementAndGetAsInt();

  /**
   * Increments the counter and gets the value as a long.
   *
   * @return the resulting value as long.
   */
  long incrementAndGetAsLong();

  /**
   * @return the value for the counter.
   */
  long getValueAsLong();

  /**
   * @return the unit for this measurement.
   */
  String getUnit();

  int getValueAsInt();

  // REVIEW NOTE: This could also be part of an internal api (I.e InternalLongCounter extends LongCounter)
  void onAddition(BiConsumer<Long, Map<String, String>> consumer);
}
