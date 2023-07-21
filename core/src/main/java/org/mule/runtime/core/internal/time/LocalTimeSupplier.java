/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.time;

import static java.lang.System.currentTimeMillis;

import org.mule.runtime.api.time.TimeSupplier;

import java.util.function.Supplier;

/**
 * A {@link Supplier} which provides the current system time in milliseconds.
 *
 * @since 4.0
 */
public class LocalTimeSupplier implements TimeSupplier {

  /**
   * Returns {@link System#currentTimeMillis()}
   *
   * @return the current time in milliseconds
   */
  @Override
  public Long get() {
    return currentTimeMillis();
  }

  @Override
  public long getAsLong() {
    return currentTimeMillis();
  }
}
