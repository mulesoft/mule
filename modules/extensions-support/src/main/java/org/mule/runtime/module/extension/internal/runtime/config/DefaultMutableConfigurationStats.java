/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import org.mule.runtime.api.time.TimeSupplier;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of {@link MutableConfigurationStats}
 *
 * @since 4.0
 */
final class DefaultMutableConfigurationStats implements MutableConfigurationStats {

  private final AtomicInteger inflightOperations = new AtomicInteger(0);
  private final AtomicInteger runningSources = new AtomicInteger(0);
  private final AtomicInteger activeComponents = new AtomicInteger(0);
  private final TimeSupplier timeSupplier;
  private long lastUsedMillis;

  /**
   * Creates a new instance using the given {@code timeSupplier} to obtain the current time and update the
   * {@link #getLastUsedMillis()}
   *
   * @param timeSupplier a {@link TimeSupplier}
   */
  public DefaultMutableConfigurationStats(TimeSupplier timeSupplier) {
    this.timeSupplier = timeSupplier;
    updateLastUsed();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long updateLastUsed() {
    return lastUsedMillis = timeSupplier.getAsLong();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getLastUsedMillis() {
    return lastUsedMillis;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getInflightOperations() {
    return inflightOperations.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getRunningSources() {
    return runningSources.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getActiveComponents() {
    return activeComponents.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int addInflightOperation() {
    updateLastUsed();
    return inflightOperations.incrementAndGet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int discountInflightOperation() {
    updateLastUsed();
    return inflightOperations.decrementAndGet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int addRunningSource() {
    addActiveComponent();
    return runningSources.incrementAndGet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int discountRunningSource() {
    discountActiveComponent();
    return runningSources.decrementAndGet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int addActiveComponent() {
    updateLastUsed();
    return activeComponents.incrementAndGet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int discountActiveComponent() {
    updateLastUsed();
    return activeComponents.decrementAndGet();
  }
}
