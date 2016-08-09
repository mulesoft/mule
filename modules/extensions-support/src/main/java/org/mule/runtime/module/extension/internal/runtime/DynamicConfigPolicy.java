/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.core.time.TimeSupplier;

/**
 * A policy for how the platform should handle dynamic configuration instances
 *
 * @since 4.0
 */
public final class DynamicConfigPolicy {

  /**
   * Returns an instance with the default settings, using the given {@code timeSupplier}
   *
   * @param timeSupplier the {@link TimeSupplier} for the returned instance
   * @return a {@link DynamicConfigPolicy} with the default settings
   */
  public static DynamicConfigPolicy getDefault(TimeSupplier timeSupplier) {
    return new DynamicConfigPolicy(ImmutableExpirationPolicy.getDefault(timeSupplier));
  }

  private final ExpirationPolicy expirationPolicy;

  /**
   * Creates a new instance.
   *
   * @param expirationPolicy the expiration policy to be used.
   * @throws IllegalArgumentException is {@code expirationPolicy} is {@code null}
   */
  public DynamicConfigPolicy(ExpirationPolicy expirationPolicy) {
    checkArgument(expirationPolicy != null, "expiration policy cannot be null");
    this.expirationPolicy = expirationPolicy;
  }

  /**
   * Returns the {@link ExpirationPolicy} for the dynamic configuration instances
   *
   * @return a {@link ExpirationPolicy}. It will never be {@code null}
   */
  public ExpirationPolicy getExpirationPolicy() {
    return expirationPolicy;
  }
}
