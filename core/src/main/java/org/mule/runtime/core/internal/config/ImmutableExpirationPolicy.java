/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import static org.mule.runtime.extension.api.ExtensionConstants.DYNAMIC_CONFIG_EXPIRATION_FREQUENCY;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.time.TimeSupplier;
import org.mule.runtime.core.internal.time.LocalTimeSupplier;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;

import java.util.concurrent.TimeUnit;

/**
 * A immutable implementation of {@link ExpirationPolicy}
 *
 * @since 4.0
 */
public final class ImmutableExpirationPolicy extends AbstractComponent implements ExpirationPolicy {

  /**
   * @return an instance with the default settings
   */
  public static ExpirationPolicy getDefault() {
    return getDefault(new LocalTimeSupplier());
  }

  /**
   * Returns an instance with the default settings
   *
   * @param timeSupplier the {@link TimeSupplier} for the returned instance to use
   * @return a {@link ExpirationPolicy} with the default settings
   */
  public static ExpirationPolicy getDefault(TimeSupplier timeSupplier) {
    return new ImmutableExpirationPolicy(DYNAMIC_CONFIG_EXPIRATION_FREQUENCY.getTime(),
                                         DYNAMIC_CONFIG_EXPIRATION_FREQUENCY.getUnit(), timeSupplier);
  }

  private final long maxIdleTime;
  private final TimeUnit timeUnit;
  private final TimeSupplier timeSupplier;

  public ImmutableExpirationPolicy(long maxIdleTime, TimeUnit timeUnit, TimeSupplier timeSupplier) {
    this.maxIdleTime = maxIdleTime;
    this.timeUnit = timeUnit;
    this.timeSupplier = timeSupplier;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isExpired(long lastUsed, TimeUnit timeUnit) {
    long idleTimeMillis = timeSupplier.get() - timeUnit.toMillis(lastUsed);
    return idleTimeMillis > this.timeUnit.toMillis(maxIdleTime);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getMaxIdleTime() {
    return maxIdleTime;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TimeUnit getTimeUnit() {
    return timeUnit;
  }
}
